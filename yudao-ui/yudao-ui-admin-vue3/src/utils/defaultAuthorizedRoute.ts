type AuthorizedRoute = {
  path?: string
  redirect?: string | { path?: string } | unknown
  meta?: {
    hidden?: boolean
  }
  children?: AuthorizedRoute[]
}

const LEGACY_HOME_ROUTES = new Set(['', '/', '/index', '/dashboard', '/tenant'])
const NOT_FOUND_ROUTE_PATH = '/:path(.*)*'
const FALLBACK_ROUTE = '/hotel/front-desk'
const PREFERRED_HOME_ROUTES = ['/hotel/front-desk', '/hotel/orders']

const isLegacyHomeRoute = (path?: string | null) => LEGACY_HOME_ROUTES.has(path || '')

const resolveRoutePath = (parentPath: string, path?: string | null) => {
  if (!path) return parentPath
  if (path.startsWith('/')) return path.replace(/\/+/g, '/')
  return `${parentPath.replace(/\/$/, '')}/${path.replace(/^\//, '')}`.replace(/\/+/g, '/')
}

const getRedirectPath = (redirect: AuthorizedRoute['redirect']) => {
  if (typeof redirect === 'string') {
    return redirect
  }
  if (redirect && typeof redirect === 'object' && 'path' in redirect) {
    return String(redirect.path || '')
  }
  return ''
}

const findFirstAuthorizedRoute = (routes: AuthorizedRoute[], parentPath = ''): string => {
  for (const route of routes) {
    if (!route?.path || route.path === NOT_FOUND_ROUTE_PATH || route.meta?.hidden) {
      continue
    }

    const currentPath = resolveRoutePath(parentPath, route.path)
    const redirectPath = getRedirectPath(route.redirect)
    const redirect = redirectPath ? resolveRoutePath(currentPath, redirectPath) : ''
    if (redirect && !isLegacyHomeRoute(redirect)) {
      return redirect
    }

    const childRoute = findFirstAuthorizedRoute(route.children || [], currentPath)
    if (childRoute) {
      return childRoute
    }

    if (!isLegacyHomeRoute(currentPath)) {
      return currentPath
    }
  }
  return ''
}

const collectAuthorizedRoutes = (routes: AuthorizedRoute[]): Set<string> => {
  const result = new Set<string>()
  const visit = (items: AuthorizedRoute[], parentPath = '') => {
    for (const route of items) {
      if (!route?.path || route.path === NOT_FOUND_ROUTE_PATH || route.meta?.hidden) {
        continue
      }
      const currentPath = resolveRoutePath(parentPath, route.path)
      const redirectPath = getRedirectPath(route.redirect)
      const redirect = redirectPath ? resolveRoutePath(currentPath, redirectPath) : ''
      if (redirect && !isLegacyHomeRoute(redirect)) {
        result.add(redirect)
      }
      if (!isLegacyHomeRoute(currentPath)) {
        result.add(currentPath)
      }
      visit(route.children || [], currentPath)
    }
  }
  visit(routes)
  return result
}

const findPreferredHomeRoute = (routes: AuthorizedRoute[]): string => {
  const authorizedRoutes = collectAuthorizedRoutes(routes)
  return PREFERRED_HOME_ROUTES.find((path) => authorizedRoutes.has(path)) || ''
}

export const resolveDefaultAuthorizedRoute = (
  targetPath: string | null | undefined,
  authorizedRoutes: AuthorizedRoute[],
  fallbackRoute = FALLBACK_ROUTE
) => {
  if (!isLegacyHomeRoute(targetPath)) {
    return targetPath || fallbackRoute
  }
  const preferredRoute = findPreferredHomeRoute(authorizedRoutes)
  if (preferredRoute) {
    return preferredRoute
  }
  return findFirstAuthorizedRoute(authorizedRoutes) || fallbackRoute
}
