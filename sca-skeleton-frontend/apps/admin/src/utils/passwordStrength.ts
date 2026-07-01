import { ZxcvbnFactory } from "@zxcvbn-ts/core";
import * as zxcvbnCommonPackage from "@zxcvbn-ts/language-common";
import * as zxcvbnZhPackage from "@zxcvbn-ts/language-zh";

const options = {
  translations: zxcvbnZhPackage.translations,
  graphs: zxcvbnCommonPackage.adjacencyGraphs,
  dictionary: {
    ...zxcvbnCommonPackage.dictionary,
    ...zxcvbnZhPackage.dictionary
  }
};

const zxcvbn = new ZxcvbnFactory(options);

export interface PasswordStrengthResult {
  score: number;
  label: string;
  color: string;
}

const STRENGTH_LABELS: Record<number, { label: string; color: string }> = {
  0: { label: "user.pwdStrengthTooWeak", color: "negative" },
  1: { label: "user.pwdStrengthWeak", color: "negative" },
  2: { label: "user.pwdStrengthMedium", color: "warning" },
  3: { label: "user.pwdStrengthStrong", color: "positive" },
  4: { label: "user.pwdStrengthVeryStrong", color: "positive" }
};

export function checkPasswordStrength(password: string): PasswordStrengthResult {
  if (!password) {
    return { score: 0, label: "user.pwdStrengthTooWeak", color: "negative" };
  }

  const result = zxcvbn.check(password);
  const { label, color } = STRENGTH_LABELS[result.score];

  return { score: result.score, label, color };
}
