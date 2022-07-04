<template>
  <v-sheet color="transparent">
    <v-container class="py-4 py-lg-8">
      <v-row>
        <v-col v-for="(item, i) in features" :key="i" cols="12" sm="6" lg="4">
          <v-card class="py-4 px-2">
            <v-responsive max-width="300" class="mx-auto text-center">
              <div class="mb-2">
                <v-icon class="text-h2" color="secondary">{{ item.icon }}</v-icon>
              </div>
              <div class="text-h6 mt-3">{{ item.title }}</div>
              <div class="font-weight-regular mt-2">{{ item.description }}</div>
            </v-responsive>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-sheet>
</template>

<script>
export default {
  data() {
    return {
      features: [
        {
          icon: "mdi-lock-open-check-outline",
          title: "Business Value Creation",
          description:
            "Learning how to navigate your operations into a data-driven future.",
        },
        {
          icon: "mdi-lightbulb-on-outline",
          title: "Data Science Consultancy",
          description:
            "Finding and unlocking the potential in your data and data operations.",
        },
        {
          icon: "mdi-magnify",
          title: "Exploratory Data Analysis",
          description:
            "Exploring specific vertices of your business that enable better informed decisions.",
        },
        {
          icon: "mdi-chart-donut-variant",
          title: "Advanced Analytics",
          description:
            "Combining multiple facets of your operations into a continues digestiable stream of knowledge.",
        },
        {
          icon: "mdi-robot-happy-outline",
          title: "Prototyping",
          description:
            "Find out what works and what not for better risk-adjusted investments into the future.",
        },
        {
          icon: "mdi-graph-outline",
          title: "Data Infrastructure",
          description:
            "Bring data solutions to production-scale and own your process.",
        },
      ],
    };
  },
};
</script>
