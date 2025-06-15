<template>
    <div class="problem-list-section">
        <h2 class="section-title">학습 현황</h2>

        <div class="problem-list-tabs">
            <button
                :class="{ active: activeTab === 'ongoing' }"
                @click="activeTab = 'ongoing'"
            >
                학습 중 ({{ ongoingProblems.length }})
            </button>
            <button
                :class="{ active: activeTab === 'completed' }"
                @click="activeTab = 'completed'"
            >
                학습 완료 ({{ completedProblems.length }})
            </button>
        </div>

        <div class="delete-mode-controls">
            <button v-if="!isSelectionMode" @click="enterSelectionMode" class="delete-mode-button">
                삭제 모드 진입
            </button>
            <template v-else>
                <button
                    @click="confirmDeleteSelectedProblems"
                    :disabled="selectedProblems.length === 0"
                    class="delete-selected-button"
                >
                    선택 삭제 ({{ selectedProblems.length }})
                </button>
                <button @click="cancelSelectionMode" class="cancel-selection-button">
                    취소
                </button>
            </template>
        </div>


        <div v-if="currentProblems.length === 0" class="no-problems-message">
            {{
                activeTab === "ongoing"
                    ? "현재 학습 중인 문제가 없습니다."
                    : "완료된 학습 문제가 없습니다."
            }}
        </div>

        <ul v-else class="problem-list">
            <li v-for="problem in currentProblems" :key="problem.id" class="problem-item">
                <ProblemListItem
                    :problem="problem"
                    :isAuthenticated="isLoggedIn"
                    :current-user-id="currentUserId"
                    :isSelectionMode="isSelectionMode"
                    :isSelected="selectedProblems.includes(problem.id)"
                    @go-to-study="goToStudy"
                    @toggle-like="handleToggleLike"
                    @toggle-scrap="handleToggleScrap"
                    @toggle-selection="handleToggleSelection" /> </li>
        </ul>
    </div>
</template>

<script>
import { ref, computed } from 'vue';
import axios from 'axios';
import ProblemListItem from '@/components/MyStudy/ProblemListItem.vue';

export default {
    name: 'ProblemListSection',
    components: {
        ProblemListItem,
    },
    props: {
        ongoingProblems: {
            type: Array,
            default: () => [],
        },
        completedProblems: {
            type: Array,
            default: () => [],
        },
        isLoggedIn: {
            type: Boolean,
            required: true,
        },
        currentUserId: {
            type: [String, Number],
            default: null,
        },
    },
    emits: ['go-to-study', 'auth-required', 'refresh-problems'],

    setup(props, { emit }) {
        const activeTab = ref('ongoing');
        const isSelectionMode = ref(false); // 삭제 모드 활성화 여부
        const selectedProblems = ref([]); // 선택된 문제 ID 목록

        const currentProblems = computed(() => {
            return activeTab.value === 'ongoing'
                ? props.ongoingProblems
                : props.completedProblems;
        });

        const goToStudy = (problemId) => {
            // 삭제 모드일 때는 문제 풀이로 이동하지 않음
            if (!isSelectionMode.value) {
                emit('go-to-study', problemId);
            }
        };

        const handleToggleLike = async (problemId) => {
            if (!props.isLoggedIn) {
                emit('auth-required');
                return;
            }
            try {
                const response = await axios.post(`/api/mystudy/problems/${problemId}/toggle-status`, {
                    userId: props.currentUserId,
                    field: 'isLiked',
                });
                if (response.data.status === 'OK') {
                    console.log(`좋아요 토글 성공: ${response.data.newStatus}, 총 좋아요: ${response.data.totalLikes}`);
                    emit('refresh-problems');
                } else {
                    console.error('좋아요 토글 실패 (서버 응답):', response.data.message);
                    alert('좋아요 상태 변경에 실패했습니다: ' + response.data.message);
                }
            } catch (error) {
                console.error('좋아요 토글 실패:', error);
                alert('좋아요 상태 변경 중 오류가 발생했습니다.');
            }
        };

        const handleToggleScrap = async (problemId) => {
            if (!props.isLoggedIn) {
                emit('auth-required');
                return;
            }
            try {
                const response = await axios.post(`/api/mystudy/problems/${problemId}/toggle-status`, {
                    userId: props.currentUserId,
                    field: 'isScrapped',
                });
                if (response.data.status === 'OK') {
                    console.log(`스크랩 토글 성공: ${response.data.newStatus}, 총 스크랩: ${response.data.totalScraps}`);
                    emit('refresh-problems');
                } else {
                    console.error('스크랩 토글 실패 (서버 응답):', response.data.message);
                    alert('스크랩 상태 변경에 실패했습니다: ' + response.data.message);
                }
            } catch (error) {
                console.error('스크랩 토글 실패:', error);
                alert('스크랩 상태 변경 중 오류가 발생했습니다.');
            }
        };

        // 삭제 모드 진입
        const enterSelectionMode = () => {
            if (!props.isLoggedIn) {
                emit('auth-required');
                return;
            }
            isSelectionMode.value = true;
            selectedProblems.value = []; // 모드 진입 시 선택 초기화
            alert("삭제할 문제를 클릭하여 선택하세요.");
        };

        // 선택 취소 및 삭제 모드 종료
        const cancelSelectionMode = () => {
            isSelectionMode.value = false;
            selectedProblems.value = [];
        };

        // ProblemListItem에서 문제 선택/해제 이벤트 처리
        const handleToggleSelection = (problemId) => {
            if (!isSelectionMode.value) return; // 삭제 모드가 아니면 선택 동작 무시

            const index = selectedProblems.value.indexOf(problemId);
            if (index > -1) {
                selectedProblems.value.splice(index, 1); // 이미 선택되어 있으면 제거
            } else {
                selectedProblems.value.push(problemId); // 선택 안 되어 있으면 추가
            }
        };

        // 선택된 문제들 일괄 삭제 확인 및 실행
        const confirmDeleteSelectedProblems = async () => {
            if (selectedProblems.value.length === 0) {
                alert("삭제할 문제를 선택해주세요.");
                return;
            }

            if (confirm(`선택된 문제 ${selectedProblems.value.length}개의 학습 현황을 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) {
                try {
                    // 선택된 각 문제에 대해 DELETE 요청 보내기
                    // Promise.all을 사용하여 여러 요청을 동시에 처리
                    const deletePromises = selectedProblems.value.map(problemId =>
                        axios.delete(`/api/mystudy/problems/${problemId}/user/${props.currentUserId}`)
                    );

                    const results = await Promise.allSettled(deletePromises);

                    let successfulDeletes = 0;
                    let failedDeletes = [];

                    results.forEach((result, index) => {
                        const problemId = selectedProblems.value[index];
                        if (result.status === 'fulfilled' && result.value.data.status === 'OK') {
                            successfulDeletes++;
                            console.log(`문제 ID ${problemId} 학습 현황 삭제 성공.`);
                        } else {
                            failedDeletes.push({
                                id: problemId,
                                error: result.reason?.response?.data?.message || result.reason?.message || '알 수 없는 오류'
                            });
                            console.error(`문제 ID ${problemId} 학습 현황 삭제 실패:`, result.reason);
                        }
                    });

                    if (successfulDeletes > 0) {
                        alert(`${successfulDeletes}개의 학습 현황이 성공적으로 삭제되었습니다.`);
                    }
                    if (failedDeletes.length > 0) {
                        const failedMessages = failedDeletes.map(f => `ID: ${f.id}, 오류: ${f.error}`).join('\n');
                        alert(`다음 문제들의 학습 현황 삭제에 실패했습니다:\n${failedMessages}`);
                    }

                    cancelSelectionMode(); // 삭제 후 선택 모드 종료 및 초기화
                    emit('refresh-problems'); // 부모에게 데이터 새로고침 요청
                } catch (error) {
                    console.error('일괄 학습 현황 삭제 중 오류 발생:', error);
                    alert('학습 현황 삭제 중 예기치 않은 오류가 발생했습니다.');
                }
            }
        };


        return {
            activeTab,
            isSelectionMode,
            selectedProblems,
            currentProblems,
            goToStudy,
            handleToggleLike,
            handleToggleScrap,
            enterSelectionMode,
            cancelSelectionMode,
            handleToggleSelection,
            confirmDeleteSelectedProblems,
        };
    },
};
</script>

<style scoped>
.problem-list-section {
    background-color: #ffffff;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    padding: 30px;
    margin-top: 40px;
    width: 100%;
    max-width: 800px;
    box-sizing: border-box;
}

.section-title {
    color: #5a2e87;
    font-size: 1.8rem;
    font-weight: 600;
    margin-bottom: 25px;
    text-align: center;
}

.problem-list-tabs {
    display: flex;
    justify-content: center;
    margin-bottom: 20px; /* 버튼과의 간격 조정 */
    gap: 15px;
}

.problem-list-tabs button {
    padding: 12px 25px;
    font-size: 1.1rem;
    font-weight: 500;
    color: #888;
    background-color: #f0f0f0;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
    min-width: 150px;
}

.problem-list-tabs button.active {
    background-color: #a471ff;
    color: white;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

.problem-list-tabs button:hover:not(.active) {
    background-color: #e0e0e0;
}

/* 삭제 모드 제어 버튼 스타일 */
.delete-mode-controls {
    display: flex;
    justify-content: center;
    margin-bottom: 30px;
    gap: 10px;
}

.delete-mode-button,
.delete-selected-button,
.cancel-selection-button {
    padding: 10px 20px;
    font-size: 1rem;
    border-radius: 8px;
    cursor: pointer;
    transition: background-color 0.2s ease, transform 0.1s ease;
}

.delete-mode-button {
    background-color: #f7a1a1; /* 삭제 모드 진입 버튼 색상 */
    color: #a30000;
    border: 1px solid #e07070;
}
.delete-mode-button:hover {
    background-color: #e07070;
    color: white;
    transform: translateY(-2px);
}

.delete-selected-button {
    background-color: #e03c3c; /* 선택 삭제 버튼 */
    color: white;
    border: none;
}
.delete-selected-button:not(:disabled):hover {
    background-color: #c90000;
    transform: translateY(-2px);
}
.delete-selected-button:disabled {
    background-color: #f0f0f0;
    color: #ccc;
    cursor: not-allowed;
}

.cancel-selection-button {
    background-color: #cccccc; /* 취소 버튼 */
    color: #333;
    border: none;
}
.cancel-selection-button:hover {
    background-color: #aaaaaa;
    transform: translateY(-2px);
}

.no-problems-message {
    text-align: center;
    color: #999;
    font-size: 1.1rem;
    padding: 50px 20px;
    border: 2px dashed #e0e0e0;
    border-radius: 8px;
    margin-top: 20px;
    background-color: #f9f9f9;
}

.problem-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 20px;
}
</style>