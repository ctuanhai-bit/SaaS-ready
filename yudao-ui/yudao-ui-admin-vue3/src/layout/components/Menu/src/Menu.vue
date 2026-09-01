<script lang="tsx">
import type { PropType } from 'vue'
import { ElMenu, ElScrollbar } from 'element-plus'
import { useAppStore } from '@/store/modules/app'
import { usePermissionStore } from '@/store/modules/permission'
import { useRenderMenuItem } from './components/useRenderMenuItem'
import { isUrl } from '@/utils/is'
import { useDesign } from '@/hooks/web/useDesign'
import type { LayoutType } from '@/types/layout'

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('menu')

export default defineComponent({
  name: 'Menu',
  props: {
    menuSelect: {
      type: Function as PropType<(index: string) => void>,
      default: undefined
    }
  },
  setup(props) {
    const appStore = useAppStore()
    const permissionStore = usePermissionStore()
    const { push, currentRoute } = useRouter()

    const layout = computed(() => appStore.getLayout)
    const collapse = computed(() => appStore.getCollapse)
    const uniqueOpened = computed(() => appStore.getUniqueOpened)
    const routers = computed(() =>
      unref(layout) === 'cutMenu' ? permissionStore.getMenuTabRouters : permissionStore.getRouters
    )
    const menuMode = computed((): 'vertical' | 'horizontal' => {
      const vertical: LayoutType[] = ['classic', 'topLeft', 'cutMenu']
      return vertical.includes(unref(layout)) ? 'vertical' : 'horizontal'
    })
    const activeMenu = computed(() => {
      const { meta, path } = unref(currentRoute)
      return (meta.activeMenu as string) || path
    })

    const menuSelect = (index: string) => {
      props.menuSelect?.(index)
      if (isUrl(index)) {
        window.open(index)
      } else {
        push(index)
      }
    }

    const renderMenu = () => (
      <ElMenu
        defaultActive={unref(activeMenu)}
        mode={unref(menuMode)}
        collapse={unref(layout) === 'top' || unref(layout) === 'cutMenu' ? false : unref(collapse)}
        uniqueOpened={unref(layout) === 'top' ? false : unref(uniqueOpened)}
        backgroundColor="var(--left-menu-bg-color)"
        textColor="var(--left-menu-text-color)"
        activeTextColor="var(--left-menu-text-active-color)"
        popperClass={
          unref(menuMode) === 'vertical'
            ? `${prefixCls}-popper--vertical`
            : `${prefixCls}-popper--horizontal`
        }
        onSelect={menuSelect}
      >
        {{
          default: () => {
            const { renderMenuItem } = useRenderMenuItem()
            return renderMenuItem(unref(routers))
          }
        }}
      </ElMenu>
    )

    return () => (
      <div
        id={prefixCls}
        class={[
          `${prefixCls} ${prefixCls}__${unref(menuMode)}`,
          'h-[100%] overflow-hidden flex-col bg-[var(--left-menu-bg-color)]',
          { 'is-mobile-collapsed': appStore.getMobile && unref(collapse) },
          {
            'w-[var(--left-menu-min-width)]': unref(collapse) && unref(layout) !== 'cutMenu',
            'w-[var(--left-menu-max-width)]': !unref(collapse) && unref(layout) !== 'cutMenu'
          }
        ]}
      >
        {unref(layout) === 'top' ? renderMenu() : <ElScrollbar>{renderMenu()}</ElScrollbar>}
      </div>
    )
  }
})
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-menu;

.#{$prefix-cls} {
  position: relative;
  transition: width var(--transition-time-02);

  :deep(.#{$elNamespace}-menu) {
    width: 100% !important;
    border-right: none;

    .is-active > .#{$elNamespace}-sub-menu__title {
      color: var(--left-menu-text-active-color) !important;
    }

    .#{$elNamespace}-sub-menu__title,
    .#{$elNamespace}-menu-item {
      &:hover {
        color: var(--left-menu-text-active-color) !important;
        background-color: var(--left-menu-bg-color) !important;
      }
    }

    .#{$elNamespace}-menu-item.is-active {
      color: var(--left-menu-text-active-color) !important;
      background-color: var(--left-menu-bg-active-color) !important;
    }

    .#{$elNamespace}-menu {
      .#{$elNamespace}-sub-menu__title,
      .#{$elNamespace}-menu-item:not(.is-active) {
        background-color: var(--left-menu-bg-light-color) !important;
      }
    }
  }

  :deep(.#{$elNamespace}-menu--collapse) {
    width: var(--left-menu-min-width);

    & > .is-active,
    & > .is-active > .#{$elNamespace}-sub-menu__title {
      background-color: var(--left-menu-collapse-bg-active-color) !important;
    }
  }

  :deep(.horizontal-collapse-transition) .#{$prefix-cls}__title {
    display: none;
  }

  &__vertical {
    :deep(.#{$elNamespace}-menu--vertical) {
      &:not(.#{$elNamespace}-menu--collapse) .#{$elNamespace}-sub-menu__title,
      .#{$elNamespace}-menu-item {
        padding-right: 0;
      }
    }
  }

  &__horizontal {
    height: var(--top-tool-height) !important;

    :deep(.#{$elNamespace}-menu--horizontal) {
      height: var(--top-tool-height);
      border-bottom: none;
    }
  }
}
</style>

<style lang="scss">
$prefix-cls: #{$namespace}-menu-popper;

.#{$prefix-cls}--vertical,
.#{$prefix-cls}--horizontal {
  .is-active > .el-sub-menu__title {
    color: var(--left-menu-text-active-color) !important;
  }

  .el-sub-menu__title,
  .el-menu-item {
    &:hover {
      color: var(--left-menu-text-active-color) !important;
      background-color: var(--left-menu-bg-color) !important;
    }
  }

  .el-menu-item.is-active {
    background-color: var(--left-menu-bg-active-color) !important;
  }
}
</style>
