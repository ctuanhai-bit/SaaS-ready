<template>
  <div :class="prefixCls" class="login-shell">
    <main class="login-shell__inner">
      <section class="login-shell__brand-panel" :aria-label="brand.name" :style="brandPanelStyle">
        <div class="login-shell__visual-content">
          <div class="login-shell__brand">
            <el-avatar
              class="login-shell__brand-logo"
              shape="square"
              :size="44"
              :src="brand.logoUrl || ''"
            >
              JD
            </el-avatar>
            <div>
              <strong>{{ brand.name }}</strong>
              <span>{{ brand.loginSubtitle }}</span>
            </div>
          </div>
          <div class="login-shell__hero">
            <p>{{ brand.loginEyebrow }}</p>
            <h1>{{ brand.loginHeadline }}</h1>
          </div>
        </div>
      </section>
      <section class="login-shell__form-panel">
        <div class="login-shell__form-content">
          <div class="login-card">
            <LoginForm />
          </div>
        </div>
        <footer class="login-shell__footer">
          <span>Hotel PMS Community</span>
          <span>开源酒店运营管理系统</span>
        </footer>
      </section>
    </main>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted } from 'vue'
import { useDesign } from '@/hooks/web/useDesign'
import { usePlatformBrand } from '@/hooks/web/usePlatformBrand'

import { LoginForm } from './components'

defineOptions({ name: 'Login' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')
const { brand, loadPlatformBrand } = usePlatformBrand()
const brandPanelStyle = computed(() =>
  brand.value.loginBackgroundUrl
    ? { backgroundImage: `url("${brand.value.loginBackgroundUrl.replace(/"/g, '\\"')}")` }
    : undefined
)

onMounted(() => loadPlatformBrand())
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-login;

.#{$prefix-cls} {
  overflow: auto;
}

.login-shell {
  min-height: 100vh;
  background: #f4f5f1;
}

.login-shell__inner {
  display: grid;
  grid-template-columns: minmax(480px, 1.08fr) minmax(440px, 0.92fr);
  min-height: 100vh;
}

.login-shell__brand-panel {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background-image: url('/login-hotel-lobby.webp');
  background-position: center;
  background-size: cover;
}

.login-shell__brand-panel::after {
  position: absolute;
  inset: 0;
  background: rgba(7, 27, 23, 0.42);
  content: '';
}

.login-shell__visual-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100%;
  padding: 48px 56px 64px;
  color: #fff;
}

.login-shell__brand {
  display: inline-flex;
  gap: 12px;
  align-items: center;
  width: fit-content;

  strong {
    display: block;
    font-size: 19px;
    line-height: 1.2;
  }

  span {
    display: block;
    margin-top: 4px;
    color: rgba(255, 255, 255, 0.76);
    font-size: 12px;
  }
}

.login-shell__brand-logo {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: #fff;
  font-weight: 800;
  background: #0f766e;
  border: 1px solid rgba(255, 255, 255, 0.36);
  border-radius: 8px;
  box-shadow: 0 14px 32px rgba(7, 27, 23, 0.28);
}

.login-shell__hero {
  max-width: 580px;

  p {
    margin: 0 0 12px;
    color: #e7d3a4;
    font-size: 14px;
    font-weight: 700;
    letter-spacing: 0;
  }

  h1 {
    margin: 0;
    font-size: 44px;
    line-height: 1.18;
    letter-spacing: 0;
    white-space: pre-line;
    text-shadow: 0 2px 18px rgba(7, 27, 23, 0.3);
  }
}

.login-shell__form-panel {
  display: grid;
  grid-template-rows: 1fr auto;
  min-height: 100vh;
  padding: 48px 64px 24px;
  background: #f4f5f1;
}

.login-shell__form-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: min(420px, 100%);
  padding: 40px;
  background: #fff;
  border: 1px solid #dfe4de;
  border-radius: 8px;
  box-shadow: 0 24px 64px rgba(22, 45, 39, 0.1);
}

.login-shell__footer {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  color: #7a847f;
  font-size: 13px;
  line-height: 1.6;

  a {
    color: #52605a;
    text-decoration: none;
    transition: color 0.2s ease;
  }

  a:hover {
    color: #0f766e;
  }
}

@media (max-width: 900px) {
  .login-shell__inner {
    grid-template-columns: 1fr;
  }

  .login-shell__brand-panel {
    min-height: 300px;
    background-position: center 58%;
  }

  .login-shell__visual-content {
    min-height: 300px;
    padding: 32px;
  }

  .login-shell__hero {
    h1 {
      font-size: 34px;
    }
  }

  .login-shell__form-panel {
    min-height: auto;
    padding: 48px 24px 24px;
  }

  .login-shell__footer {
    margin-top: 40px;
  }
}

@media (max-width: 560px) {
  .login-shell__brand-panel {
    min-height: 240px;
  }

  .login-shell__visual-content {
    min-height: 240px;
    padding: 24px;
  }

  .login-shell__brand-logo {
    width: 40px;
    height: 40px;
  }

  .login-shell__hero {
    p {
      margin-bottom: 8px;
    }

    h1 {
      font-size: 29px;
    }
  }

  .login-shell__form-panel {
    padding: 32px 16px 20px;
  }

  .login-card {
    padding: 28px 20px;
  }

  .login-shell__footer {
    flex-wrap: wrap;
    gap: 4px 8px;
    margin-top: 32px;
    text-align: center;
  }
}
</style>

<style lang="scss">
.dark .login-form {
  .el-divider__text {
    background-color: var(--login-bg-color);
  }

  .el-card {
    background-color: var(--login-bg-color);
  }
}
</style>
