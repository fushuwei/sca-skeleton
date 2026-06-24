import { defineStore } from "pinia";
import { ref, type Component } from "vue";

export const useRightDrawerStore = defineStore("rightDrawer", () => {
  const open = ref(false);
  const title = ref("");
  const icon = ref("sym_r_widgets");
  const contentComponent = ref<Component | null>(null);
  const contentProps = ref<Record<string, unknown>>({});

  function openDrawer(
    newTitle: string,
    newIcon: string,
    component: Component | null = null,
    props: Record<string, unknown> = {}
  ) {
    title.value = newTitle;
    icon.value = newIcon;
    contentComponent.value = component;
    contentProps.value = props;
    open.value = true;
  }

  function closeDrawer() {
    open.value = false;
    contentComponent.value = null;
    contentProps.value = {};
  }

  return {
    open,
    title,
    icon,
    contentComponent,
    contentProps,
    openDrawer,
    closeDrawer
  };
});
