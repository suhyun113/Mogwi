<template>
    <div class="problem-list-section">
        <div class="section-header">
            <h2 class="section-title">
                <template v-if="isLoggedIn">
                    <span class="username-underline">{{ username }}</span>님의 학습 현황
                </template>
                <template v-else>
                    학습 현황
                </template>
            </h2>
            <div class="header-buttons">
                <button
                    v-if="!isSelectionMode"
                    @click="enterSelectionMode"
                    class="action-button delete-mode-icon-button"
                    :disabled="!isLoggedIn || currentProblems.length === 0" aria-label="삭제 모드 진입"
                >
                    <img :src="deleteIcon" alt="삭제 아이콘" class="button-icon" />
                    <span>삭제</span>
                </button>
                <template v-else>
                    <button
                        @click="confirmDeleteSelectedProblems"
                        :disabled="selectedProblems.length === 0 || !isLoggedIn"
                        class="action-button delete-selected-button"
                        :class="{ 'is-active': selectedProblems.length > 0 }"
                        aria-label="선택된 문제 삭제"
                    >
                        <img :src="deleteIcon" alt="삭제 아이콘" class="button-icon" />
                        <span>선택 삭제 ({{ selectedProblems.length }})</span>
                    </button>
                    <button
                        @click="cancelSelectionMode"
                        class="action-button cancel-selection-icon-button"
                        :disabled="!isLoggedIn"
                        aria-label="선택 취소"
                    >
                        <img :src="deleteIcon" alt="취소 아이콘" class="button-icon" />
                        <span>취소</span>
                    </button>
                </template>
            </div>
        </div>

        <div class="problem-list-tabs">
            <button
                :class="{ active: activeTab === 'ongoing' }"
                @click="activeTab = 'ongoing'"
                :disabled="!isLoggedIn"
            >
                학습 중 ({{ isLoggedIn ? ongoingProblems.length : 0 }})
            </button>
            <button
                :class="{ active: activeTab === 'completed' }"
                @click="activeTab = 'completed'"
                :disabled="!isLoggedIn"
            >
                학습 완료 ({{ isLoggedIn ? completedProblems.length : 0 }})
            </button>
        </div>

        <div v-if="!isLoggedIn" class="no-problems-message not-logged-in-msg">
            <p>로그인하시면 학습 현황을 확인하실 수 있습니다.</p>
            <p>문제 추가, 수정, 삭제 등의 기능을 이용하실 수 있습니다.</p>
        </div>
        <div v-else-if="currentProblems.length === 0" class="no-problems-message">
            {{
                activeTab === "ongoing"
                    ? "현재 학습 중인 문제가 없습니다."
                    : "완료된 학습 문제가 없습니다."
            }}
        </div>

        <ul v-else class="problem-list">
            <li v-for="problem in paginatedProblems" :key="problem.id" class="problem-item">
                <ProblemListItem
                    :problem="problem"
                    :isAuthenticated="isLoggedIn"
                    :current-user-id="currentUserId"
                    :isSelectionMode="isSelectionMode"
                    :isSelected="selectedProblems.includes(problem.id)"
                    @go-to-study="goToStudy"
                    @toggle-like="handleToggleLike"
                    @toggle-scrap="handleToggleScrap"
                    @toggle-selection="handleToggleSelection"
                    @auth-required="$emit('auth-required')" />
            </li>
        </ul>
        <div v-if="totalPages > 0" class="pagination">
            <button class="pagination-arrow" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">&lt;</button>
            <button
                v-for="page in totalPages"
                :key="page"
                class="pagination-btn"
                :class="{ active: page === currentPage }"
                @click="goToPage(page)"
            >
                {{ page }}
            </button>
            <button class="pagination-arrow" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">&gt;</button>
        </div>
    </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import axios from 'axios';
import ProblemListItem from '@/components/MyStudy/ProblemListItem.vue';
import deleteIcon from '@/assets/icons/delete.png';

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
        username: {
            type: String,
            default: '사용자',
        },
    },
    emits: ['go-to-study', 'auth-required', 'refresh-problems'],

    setup(props, { emit }) {
        const activeTab = ref('ongoing');
        const isSelectionMode = ref(false);
        const selectedProblems = ref([]);

        const problemsPerPage = 3;
        const currentPage = ref(1);

        const currentProblems = computed(() => {
            if (!props.isLoggedIn) {
                return [];
            }
            return activeTab.value === 'ongoing'
                ? props.ongoingProblems
                : props.completedProblems;
        });

        const totalPages = computed(() => {
            return Math.max(1, Math.ceil(currentProblems.value.length / problemsPerPage));
        });

        const paginatedProblems = computed(() => {
            const start = (currentPage.value - 1) * problemsPerPage;
            return currentProblems.value.slice(start, start + problemsPerPage);
        });

        watch(currentProblems, () => {
            currentPage.value = 1; // 탭 변경 시 첫 페이지로
        });

        const checkLoginAndExecute = () => {
            if (!props.isLoggedIn) {
                emit('auth-required');
                return false;
            }
            return true;
        };

        const goToStudy = (problemId) => {
            if (!checkLoginAndExecute()) return;
            if (!isSelectionMode.value) {
                emit('go-to-study', problemId);
            }
        };

        const handleToggleLike = async (problemId) => {
            if (!checkLoginAndExecute()) return;

            // 해당 문제를 현재 탭에서 찾음
            const list = currentProblems.value;
            const target = list.find(p => p.id === problemId);
            if (!target) return;

            try {
                const response = await axios.post(`/api/like/${problemId}`, {
                userId: props.currentUserId
                });

                if (response.data.status === 'OK') {
                // 👉 좋아요 상태와 수만 업데이트
                target.isLiked = !target.isLiked;
                target.totalLikes += target.isLiked ? 1 : -1;
                } else {
                alert('좋아요 상태 변경에 실패했습니다: ' + response.data.message);
                }
            } catch (error) {
                console.error('좋아요 토글 실패:', error);
                alert('좋아요 상태 변경 중 오류가 발생했습니다.');
            }
        };


        const handleToggleScrap = async (problemId) => {
            if (!checkLoginAndExecute()) return;
            try {
                const response = await axios.post(`/api/scrap/${problemId}/toggle-scrap`, {
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

        const enterSelectionMode = () => {
            if (!checkLoginAndExecute()) return;
            // Additional check to disable the button if there are no problems in either tab
            if (props.ongoingProblems.length === 0 && props.completedProblems.length === 0) {
                alert("삭제할 문제가 없습니다.");
                return;
            }
            isSelectionMode.value = true;
            selectedProblems.value = [];
            alert("삭제할 문제를 클릭하여 선택한 후, '선택 삭제' 버튼을 눌러주세요.");
        };

        const cancelSelectionMode = () => {
            isSelectionMode.value = false;
            selectedProblems.value = [];
        };

        const handleToggleSelection = (problemId) => {
            if (!checkLoginAndExecute()) return;
            if (!isSelectionMode.value) return;

            const index = selectedProblems.value.indexOf(problemId);
            if (index > -1) {
                selectedProblems.value.splice(index, 1);
            } else {
                selectedProblems.value.push(problemId);
            }
        };

        const confirmDeleteSelectedProblems = async () => {
            if (!checkLoginAndExecute()) return;

            if (selectedProblems.value.length === 0) {
                alert("삭제할 문제를 선택해주세요.");
                return;
            }

            if (confirm(`선택된 문제 ${selectedProblems.value.length}개의 학습 현황을 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) {
                try {
                    const deletePromises = selectedProblems.value.map(problemId =>
                        axios.delete(`/api/mystudy/problems/${problemId}/status/${props.currentUserId}`)
                    );

                    const results = await Promise.allSettled(deletePromises);

                    let successfulDeletes = 0;
                    let failedDeletes = [];
                    let infoMessages = []; // To collect "INFO" messages

                    results.forEach((result, index) => {
                        const problemId = selectedProblems.value[index];
                        if (result.status === 'fulfilled') {
                            if (result.value.data.status === 'OK') {
                                successfulDeletes++;
                                console.log(`문제 ID ${problemId} 학습 현황 삭제 성공.`);
                            } else if (result.value.data.status === 'INFO') {
                                infoMessages.push(`문제 ID ${problemId}: ${result.value.data.message}`);
                                console.log(`문제 ID ${problemId} 학습 현황 정보:`, result.value.data.message);
                            } else {
                                failedDeletes.push({
                                    id: problemId,
                                    error: result.value.data.message || '알 수 없는 오류'
                                });
                                console.error(`문제 ID ${problemId} 학습 현황 삭제 실패 (서버 응답):`, result.value.data.message);
                            }
                        } else { // result.status === 'rejected'
                            failedDeletes.push({
                                id: problemId,
                                error: result.reason?.response?.data?.message || result.reason?.message || '네트워크 오류 또는 서버 응답 없음'
                            });
                            console.error(`문제 ID ${problemId} 학습 현황 삭제 실패 (네트워크/서버 오류):`, result.reason);
                        }
                    });

                    let summaryMessage = '';
                    if (successfulDeletes > 0) {
                        summaryMessage += `${successfulDeletes}개의 학습 현황이 성공적으로 삭제되었습니다.\n`;
                    }
                    if (infoMessages.length > 0) {
                        summaryMessage += `\n주의: 일부 문제는 이미 학습 상태가 없었습니다.\n${infoMessages.join('\n')}\n`;
                    }
                    if (failedDeletes.length > 0) {
                        const failedMessages = failedDeletes.map(f => `ID: ${f.id}, 오류: ${f.error}`).join('\n');
                        summaryMessage += `\n다음 문제들의 학습 현황 삭제에 실패했습니다:\n${failedMessages}`;
                    }

                    if (summaryMessage) {
                        alert(summaryMessage.trim());
                    } else {
                        alert("선택된 문제들의 학습 현황을 삭제할 수 없었습니다.");
                    }

                    cancelSelectionMode();
                    emit('refresh-problems');
                } catch (error) {
                    console.error('일괄 학습 현황 삭제 중 예기치 않은 오류 발생:', error);
                    alert('학습 현황 삭제 중 예기치 않은 오류가 발생했습니다.');
                }
            }
        };

        function goToPage(page) {
            if (page >= 1 && page <= totalPages.value) {
                currentPage.value = page;
            }
        }

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
            deleteIcon,
            totalPages,
            paginatedProblems,
            currentPage,
            goToPage,
        };
    },
};
</script>

<style scoped>
/* 기존 스타일 유지 */
.problem-list-section {
    background-color: #ffffff;
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    padding: 30px 30px 50px 30px; /* 하단 패딩 50px로 증가 */
    margin-top: -20px;
    width: 100%;
    max-width: 800px;
    box-sizing: border-box;
    border: 1px solid #e0d0ff; /* Subtle border */
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0;
}

.section-title {
    color: #5a2e87;
    font-size: 1.8rem;
    font-weight: 600;
    margin: 5;
    text-align: left; /* Ensure left alignment */
}

/* Add style for the username underline */
.username-underline {
    text-decoration: underline;
    text-underline-offset: 4px; /* Adjust this value as needed for better spacing */
    text-decoration-color: #a471ff; /* Optional: Change underline color */
    text-decoration-thickness: 2px; /* Optional: Make underline thicker */
}


.header-buttons {
    display: flex;
    gap: 15px;
    align-items: center;
}

/* 모든 액션 버튼에 공통적으로 적용될 스타일 */
.action-button {
    background: none;
    border: none;
    cursor: pointer;
    padding: 5px 10px;
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 1rem;
    font-weight: 500;
    transition: color 0.2s ease, transform 0.1s ease;
}

.action-button:hover:not(:disabled) {
    transform: translateY(-1px);
}

.button-icon {
    width: 24px;
    height: 24px;
    vertical-align: middle;
    filter: brightness(1); /* 기본 밝기 설정 */
    transition: filter 0.2s ease, opacity 0.2s ease; /* 필터 애니메이션 추가 */
}

/* 1. "삭제" 버튼 (기본 상태) */
.delete-mode-icon-button {
    color:rgb(157, 28, 28); /* 빨간색 텍스트 */
}
.delete-mode-icon-button:hover:not(:disabled) {
    color: #c90000;
}
.delete-mode-icon-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    color: #a0a0a0;
}
.delete-mode-icon-button:disabled .button-icon {
    filter: grayscale(100%) brightness(150%); /* 아이콘을 밝은 회색조로 */
    opacity: 0.6;
}


/* 2. "선택 삭제 (N)" 버튼 (삭제 모드 시) */
.delete-selected-button {
    color: #a0a0a0; /* 기본 텍스트 색상: 회색 */
}
.delete-selected-button .button-icon {
    filter: grayscale(100%) brightness(150%); /* 기본 아이콘 색상: 회색 */
}

.delete-selected-button.is-active { /* 선택된 문제가 있을 때 활성화 */
    color: #e03c3c; /* 빨간색 텍스트 */
}
.delete-selected-button.is-active .button-icon {
    filter: invert(21%) sepia(87%) saturate(3061%) hue-rotate(344deg) brightness(80%) contrast(100%); /* 빨간색 필터 */
}
.delete-selected-button.is-active:hover:not(:disabled) {
    color: #c90000; /* 호버 시 더 진한 빨간색 */
}

.delete-selected-button:disabled { /* 로그인 안 했거나 문제가 없을 때 */
    opacity: 0.6;
    cursor: not-allowed;
}
/* disabled 상태는 .is-active 여부와 관계없이 적용 */
.delete-selected-button:disabled .button-icon {
    filter: grayscale(100%) brightness(150%); /* 비활성화 시 아이콘 회색 */
    opacity: 0.6;
}


/* 3. "취소" 버튼 (삭제 모드 시) */
.cancel-selection-icon-button {
    color: #555; /* 회색 텍스트 */
}
.cancel-selection-icon-button .button-icon {
    filter: grayscale(100%) brightness(100%); /* 회색 필터 (조금 더 어둡게) */
}
.cancel-selection-icon-button:hover:not(:disabled) {
    color: #333; /* 호버 시 더 진한 회색 */
}
.cancel-selection-icon-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    color: #a0a0a0;
}
.cancel-selection-icon-button:disabled .button-icon {
    filter: grayscale(100%) brightness(150%); /* 비활성화 시 아이콘 밝은 회색 */
    opacity: 0.6;
}

.problem-list-tabs {
    display: flex;
    justify-content: center;
    margin: -10px 0 20px 0;
    gap: 15px;
}

.problem-list-tabs button {
    padding: 8px 20px;
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

.problem-list-tabs button:hover:not(.active):not(:disabled) {
    background-color: #e0e0e0;
}
.problem-list-tabs button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    background-color: #f0f0f0;
    color: #a0a0a0;
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
.no-problems-message.not-logged-in-msg {
    border: 2px dashed #ffb3b3;
    background-color: #fff0f0;
    color: #cc0000;
    font-weight: 500;
}
.no-problems-message.not-logged-in-msg p {
    margin-bottom: 10px;
}
.no-problems-message.not-logged-in-msg p:last-child {
    margin-bottom: 0;
}

.problem-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 0;
}

.problem-item {
    margin-bottom: -1px; /* 아이템들을 약간 겹치게 만듦 */
}

.pagination {
    display: flex;
    justify-content: center;
    gap: 8px;
    margin-top: 0px;
    margin-bottom: 20px; /* 페이지 번호 아래에 20px 여백 추가 */
    align-items: center;
}
.pagination-btn {
    background: none; /* 배경 제거 */
    border: none; /* 테두리 제거 */
    color: #5a2e87; /* 텍스트 색상 유지 */
    border-radius: 0; /* 둥근 테두리 제거 */
    padding: 0 5px; /* 패딩 최소화 */
    font-size: 1.1rem; /* 폰트 크기 유지 */
    font-weight: 600; /* 폰트 굵기 좀 더 강조 */
    cursor: pointer;
    transition: all 0.2s;
}
.pagination-btn.active {
    color: #a471ff; /* 활성화된 페이지 색상 강조 */
    text-decoration: underline; /* 활성화된 페이지 밑줄 */
}
.pagination-btn:hover:not(.active):not(:disabled) {
    color: #854fe6; /* 호버 시 색상 변경 */
    text-decoration: underline; /* 호버 시 밑줄 */
}
.pagination-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    color: #a0a0a0; /* 비활성화 시 색상 */
}
.pagination-arrow {
    font-size: 1.2rem;
    font-weight: bold;
    background: none;
    border: none;
    color: #a471ff;
    padding: 0 10px;
    cursor: pointer;
    transition: color 0.2s;
}
</style>