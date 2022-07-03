<template>
  <v-app>
    <v-main>
      <Toolbar></Toolbar>
      <nuxt />
      <Footer></Footer>
    </v-main>
  </v-app>
</template>

<script>
import Toolbar from '@/components/ui/landing/toolbar/Toolbar5.vue'
import Footer from '@/components/ui/landing/footer/Footer8.vue'

export default {
  components: {
    Toolbar,
    Footer
  }
}
</script>
