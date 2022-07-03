<template>
  <div>
    <Hero></Hero>
    <Partners></Partners>
    <Contact></Contact>
  </div>
</template>

<script>
import Hero from "@/components/ui/landing/hero/Hero8.vue";
import Partners from "@/components/ui/landing/logos/Logos2Demo.vue";
import Contact from "@/components/ui/landing/contact/Contact4.vue";

export default {
  components: {
    Hero,
    Contact,
    Partners,
  },
};
</script>
