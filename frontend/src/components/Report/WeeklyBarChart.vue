<template>
  <div class="weekly-bar-chart">
    <canvas ref="chartCanvas" v-if="chartData.length > 0"></canvas>
    <div v-else class="no-chart-data">
        <img src="@/assets/mogwi-confused.png" alt="모귀 혼란" class="mogwi-confused-icon" />
        <p>지난 주간 학습 기록이 없습니다.</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue';
import Chart from 'chart.js/auto';
import mogwiConfused from '@/assets/mogwi-confused.png';

export default {
  name: 'WeeklyBarChart',
  props: {
    chartData: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    const chartCanvas = ref(null);
    let chartInstance = null;

    const createChart = () => {
      if (chartInstance) {
        chartInstance.destroy();
      }

      if (!chartCanvas.value || props.chartData.length === 0) {
        return;
      }

      const labels = props.chartData.map(d => d.label);
      const perfectData = props.chartData.map(d => d.perfect);
      const vagueData = props.chartData.map(d => d.vague);
      const forgottenData = props.chartData.map(d => d.forgotten);

      const ctx = chartCanvas.value.getContext('2d');
      chartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [
            {
              label: '완벽한 기억',
              data: perfectData,
              backgroundColor: '#8BC34A',
              borderColor: '#689F38',
              borderWidth: 1,
              stack: 'study',
            },
            {
              label: '희미한 기억',
              data: vagueData,
              backgroundColor: '#FFEB3B',
              borderColor: '#FBC02D',
              borderWidth: 1,
              stack: 'study',
            },
            {
              label: '사라진 기억',
              data: forgottenData,
              backgroundColor: '#F44336',
              borderColor: '#D32F2F',
              borderWidth: 1,
              stack: 'study',
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            tooltip: {
              mode: 'index',
              intersect: false,
              callbacks: {
                title: function(tooltipItems) {
                    return tooltipItems[0].label;
                },
                label: function(tooltipItem) {
                    const datasetLabel = tooltipItem.dataset.label || '';
                    const value = tooltipItem.raw;
                    return `${datasetLabel}: ${value} 카드`;
                },
                footer: function(tooltipItems) {
                    const total = tooltipItems.reduce((sum, item) => sum + item.raw, 0);
                    return `총 학습 카드: ${total}`;
                }
              }
            },
            legend: {
              display: true,
              position: 'bottom',
              labels: {
                boxWidth: 20,
                padding: 15,
                font: {
                  size: 12,
                  family: 'Pretendard',
                },
                color: '#555',
              }
            }
          },
          scales: {
            x: {
              stacked: true,
              title: {
                display: true,
                text: '주차',
                color: '#5a2e87',
                font: {
                  size: 14,
                  weight: 'bold',
                  family: 'Pretendard',
                }
              },
              ticks: {
                color: '#666',
                font: {
                    family: 'Pretendard',
                }
              },
              grid: {
                display: false,
              }
            },
            y: {
              stacked: true,
              beginAtZero: true,
              title: {
                display: true,
                text: '학습 카드 수',
                color: '#5a2e87',
                font: {
                  size: 14,
                  weight: 'bold',
                  family: 'Pretendard',
                }
              },
              ticks: {
                color: '#666',
                stepSize: 10,
                font: {
                    family: 'Pretendard',
                }
              },
              grid: {
                color: '#e0e0e0',
              }
            }
          }
        }
      });
    };

    onMounted(() => {
      createChart();
    });

    watch(() => props.chartData, () => {
      createChart();
    }, { deep: true });

    onBeforeUnmount(() => {
      if (chartInstance) {
        chartInstance.destroy();
      }
    });

    return {
      chartCanvas,
      mogwiConfused
    };
  }
};
</script>

<style scoped>
.weekly-bar-chart {
  width: 100%;
  height: 400px;
  padding: 0; /* ReportView의 .report-section에서 padding을 처리하므로 여기서는 0으로 */
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-bottom: 0px;
}

canvas {
  width: 100% !important;
  height: 100% !important;
}

.no-chart-data {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background-color: rgba(255, 255, 255, 0.9);
    border-radius: 10px;
    z-index: 10;
    color: #888;
    font-size: 1.2rem;
    text-align: center;
}

.mogwi-confused-icon {
    width: 110px;
    height: auto;
    margin-bottom: 12px;
}

@media (max-width: 768px) {
  .weekly-bar-chart {
    height: 300px;
    padding: 0; /* ReportView에서 처리 */
  }
  .mogwi-confused-icon {
      width: 70px;
  }
  .no-chart-data {
      font-size: 0.95rem;
  }
}
</style>