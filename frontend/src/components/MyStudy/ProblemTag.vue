<template>
  <span
    class="tag"
    :style="{ backgroundColor: backgroundColor }"
  >
    {{ tagName }}
  </span>
</template>

<script>
export default {
  name: 'ProblemTag',
  props: {
    tagName: {
      type: String,
      required: true
    },
    backgroundColor: { // Prop for the background color determined by the parent
      type: String,
      default: '#ccc' // Fallback color
    }
  }
};
</script>

<style scoped>
.tag {
  padding: 4px 10px; /* Adjusted to match your ProblemListItem/ProblemSummary style */
  border-radius: 14px; /* Adjusted to match your ProblemListItem/ProblemSummary style */
  font-size: 13px; /* Adjusted to match your ProblemListItem/ProblemSummary style */
  color: white; /* Tags in ProblemListItem/ProblemSummary have white text */
  white-space: nowrap;
}
</style>