/**
 * 客户端 JDBC 驱动类探测。
 *
 * 直接在浏览器中读取 JAR（ZIP 格式）中的 META-INF/services/java.sql.Driver 声明文件，
 * 与原后端探测的主策略等价（仅读取 services 声明，不加载类）。文件无需先行上传到服务端，
 * 选中文件即可即时得到候选驱动类，供用户一键回填驱动类名输入框。
 *
 * 说明：
 * - 仅实现 ZIP 中心目录解析（不支持 ZIP64），超大/非标准 JAR 探测失败时静默跳过，
 *   由用户手动输入驱动类名（表单本身就支持手动输入）。
 * - DEFLATE 解压使用浏览器原生 DecompressionStream("deflate-raw")，无第三方依赖。
 */

const EOCD_SIGNATURE = 0x06054b50;
const CENTRAL_DIR_SIGNATURE = 0x02014b50;
const LOCAL_HEADER_SIGNATURE = 0x04034b50;
const SERVICES_ENTRY_NAME = "META-INF/services/java.sql.Driver";

/** EOCD 固定 22 字节 + 注释最长 65535 字节 */
const EOCD_MAX_SEARCH = 22 + 65535;

/** 单个 JAR 最多返回的候选类数量 */
const MAX_RESULTS_PER_JAR = 20;

const CLASS_NAME_PATTERN = /^[A-Za-z_$][\w$]*(\.[A-Za-z_$][\w$]*)*$/;

interface ServicesEntryLocation {
  compressionMethod: number;
  compressedSize: number;
  localHeaderOffset: number;
}

/**
 * 探测多个 JAR 文件中声明的 JDBC 驱动类（跨文件去重，保持发现顺序）。
 */
export async function detectDriverClassesInJars(files: File[]): Promise<string[]> {
  const result: string[] = [];
  for (const file of files) {
    if (!file.name.toLowerCase().endsWith(".jar")) {
      continue;
    }
    try {
      const classes = await detectInSingleJar(file);
      for (const cls of classes) {
        if (!result.includes(cls)) {
          result.push(cls);
        }
      }
    } catch {
      // 单个文件探测失败（非标准 ZIP / ZIP64 / 读取异常）不影响整体，跳过即可
    }
  }
  return result;
}

async function detectInSingleJar(file: File): Promise<string[]> {
  const location = await findServicesEntry(file);
  if (!location) {
    return [];
  }
  const text = await readEntryContent(file, location);
  return parseServicesContent(text);
}

/**
 * 解析 ZIP 中心目录，定位 META-INF/services/java.sql.Driver 条目。
 */
async function findServicesEntry(file: File): Promise<ServicesEntryLocation | null> {
  const tailSize = Math.min(file.size, EOCD_MAX_SEARCH);
  if (tailSize < 22) {
    return null;
  }
  const tail = new DataView(await file.slice(file.size - tailSize).arrayBuffer());

  // 从尾部向前查找 EOCD 签名
  let eocdPos = -1;
  for (let pos = tail.byteLength - 22; pos >= 0; pos--) {
    if (tail.getUint32(pos, true) === EOCD_SIGNATURE) {
      eocdPos = pos;
      break;
    }
  }
  if (eocdPos < 0) {
    return null;
  }

  const centralDirOffset = tail.getUint32(eocdPos + 16, true);
  const centralDirSize = tail.getUint32(eocdPos + 12, true);
  const entryCount = tail.getUint16(eocdPos + 10, true);
  // ZIP64（偏移为 0xFFFFFFFF）不在支持范围
  if (centralDirOffset === 0xffffffff || centralDirSize === 0xffffffff) {
    return null;
  }

  const centralDir = new DataView(
    await file.slice(centralDirOffset, centralDirOffset + centralDirSize).arrayBuffer()
  );

  let pos = 0;
  for (let i = 0; i < entryCount && pos + 46 <= centralDir.byteLength; i++) {
    if (centralDir.getUint32(pos, true) !== CENTRAL_DIR_SIGNATURE) {
      return null;
    }
    const compressionMethod = centralDir.getUint16(pos + 10, true);
    const compressedSize = centralDir.getUint32(pos + 20, true);
    const nameLength = centralDir.getUint16(pos + 28, true);
    const extraLength = centralDir.getUint16(pos + 30, true);
    const commentLength = centralDir.getUint16(pos + 32, true);
    const localHeaderOffset = centralDir.getUint32(pos + 42, true);

    const nameBytes = new Uint8Array(centralDir.buffer, centralDir.byteOffset + pos + 46, nameLength);
    const entryName = new TextDecoder("utf-8").decode(nameBytes);

    if (entryName === SERVICES_ENTRY_NAME) {
      return { compressionMethod, compressedSize, localHeaderOffset };
    }
    pos += 46 + nameLength + extraLength + commentLength;
  }
  return null;
}

/**
 * 按中心目录记录读取条目内容并解压（条目实际大小以中心目录为准，
 * 本地文件头在启用数据描述符时其大小字段可能为 0）。
 */
async function readEntryContent(file: File, location: ServicesEntryLocation): Promise<string> {
  const header = new DataView(await file.slice(location.localHeaderOffset, location.localHeaderOffset + 30).arrayBuffer());
  if (header.byteLength < 30 || header.getUint32(0, true) !== LOCAL_HEADER_SIGNATURE) {
    throw new Error("Invalid local file header");
  }
  const nameLength = header.getUint16(26, true);
  const extraLength = header.getUint16(28, true);
  const dataStart = location.localHeaderOffset + 30 + nameLength + extraLength;

  const compressed = await file.slice(dataStart, dataStart + location.compressedSize).arrayBuffer();

  let content: ArrayBuffer;
  if (location.compressionMethod === 0) {
    // STORED：无压缩
    content = compressed;
  } else if (location.compressionMethod === 8) {
    // DEFLATE：浏览器原生流式解压
    const stream = new Blob([compressed]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
    content = await new Response(stream).arrayBuffer();
  } else {
    throw new Error("Unsupported compression method: " + location.compressionMethod);
  }
  return new TextDecoder("utf-8").decode(content);
}

/**
 * 解析 services 声明文件内容：每行一个类全限定名，# 开头为注释。
 */
function parseServicesContent(text: string): string[] {
  const classes: string[] = [];
  for (const rawLine of text.split(/\r?\n/)) {
    // services 文件允许行内注释（类名后跟 # ...）
    const line = rawLine.split("#")[0].trim();
    if (!line) {
      continue;
    }
    if (CLASS_NAME_PATTERN.test(line) && !classes.includes(line)) {
      classes.push(line);
    }
    if (classes.length >= MAX_RESULTS_PER_JAR) {
      break;
    }
  }
  return classes;
}
