// Composables
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/cortana',
    component: () => import('@/layouts/default/Default.vue'),
    children: [
      {
        path: '',
        name: 'Results',
        props: true,
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "results" */ '@/components/ResultList.vue'),
      },
      {
        path: '',
        name: 'Silences',
        props: true,
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "silences" */ '@/components/SilencesList.vue'),
      },
      {
        path: '',
        name: 'Routes',
        props: true,
        component: () => import(/* webpackChunkName: "routes" */ '@/components/RoutesList.vue'),
      },
      {
        path: '',
        name: 'Users',
        props: true,
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "silences" */ '@/components/UserList.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
})

function hasQueryParams(route) {
  return !!Object.keys(route.query).length
}

router.beforeEach((to, from, next) => {
   if(!hasQueryParams(to) && hasQueryParams(from)){
    next({name: to.name, query: from.query});
  } else {
    next()
  }
})

export default router
