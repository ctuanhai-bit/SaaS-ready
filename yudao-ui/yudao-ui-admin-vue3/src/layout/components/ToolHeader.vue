<script lang="tsx">
import { defineComponent, computed } from 'vue'
import { Collapse } from '@/layout/components/Collapse'
import { UserInfo } from '@/layout/components/UserInfo'
import { Breadcrumb } from '@/layout/components/Breadcrumb'
import TenantVisit from '@/layout/components/TenantVisit/index.vue'
import { useAppStore } from '@/store/modules/app'
import { useDesign } from '@/hooks/web/useDesign'
import { checkPermi } from '@/utils/permission'

const { getPrefixCls, variables } = useDesign()

const prefixCls = getPrefixCls('tool-header')

const appStore = useAppStore()

// 面包屑
const breadcrumb = computed(() => appStore.getBreadcrumb)

// 折叠图标
const hamburger = computed(() => appStore.getHamburger)

// 布局
const layout = computed(() => appStore.getLayout)

// 租户切换权限
const hasTenantVisitPermission = computed(
  () =>
    import.meta.env.VITE_APP_TENANT_ENABLE === 'true' &&
    checkPermi(['system:tenant:visit'])
)

export default defineComponent({
  name: 'ToolHeader',
  setup() {
    return () => (
      <div
        id={`${variables.namespace}-tool-header`}
        class={[
          prefixCls,
          'h-[var(--top-tool-height)] relative px-[var(--top-tool-p-x)] flex items-center justify-between',
          'dark:bg-[var(--el-bg-color)]'
        ]}
      >
        {layout.value !== 'top' ? (
          <div class="h-full flex items-center">
            {hamburger.value && layout.value !== 'cutMenu' ? (
              <Collapse class="custom-hover" color="var(--top-header-text-color)"></Collapse>
            ) : undefined}
            {breadcrumb.value ? <Breadcrumb class="lt-md:hidden"></Breadcrumb> : undefined}
          </div>
        ) : undefined}
        <div class="h-full flex items-center">
          {hasTenantVisitPermission.value ? <TenantVisit /> : undefined}
          <UserInfo></UserInfo>
        </div>
      </div>
    )
  }
})
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-tool-header;

.#{$prefix-cls} {
  color: var(--saas-text);
  background: var(--saas-surface);
  border-bottom: 1px solid var(--saas-border);
  box-shadow: 0 4px 18px rgb(22 45 39 / 4%);
  transition: left var(--transition-time-02);
}

.saas-backend-switch {
  display: inline-flex;
  height: 100%;
  align-items: center;
}

.saas-backend-switch__trigger {
  display: inline-flex;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 700;
  line-height: 18px;
  color: var(--saas-primary-dark);
  cursor: pointer;
  background: var(--saas-primary-soft);
  border: 1px solid #bfd3cb;
  border-radius: 6px;
  align-items: center;
  gap: 6px;

  &:hover {
    background: #dcebe7;
    border-color: var(--saas-primary);
  }
}

.saas-backend-switch__item {
  display: flex;
  min-width: 150px;
  align-items: center;
  gap: 8px;
}

.saas-backend-switch__current {
  margin-left: auto;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  color: var(--saas-primary);
}
</style>
