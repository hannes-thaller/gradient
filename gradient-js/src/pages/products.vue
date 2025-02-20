<template>
<div>
  <FeatureHero></FeatureHero>
  <FeatureBangs></FeatureBangs>
</div>
</template>

<script>
import FeatureHero from "@/components/ui/landing/feature/Feature1.vue";
import FeatureBangs from "@/components/ui/landing/feature/Feature7.vue";

export default {
  components: {
    FeatureHero,
    FeatureBangs,
  },
};
</script>
