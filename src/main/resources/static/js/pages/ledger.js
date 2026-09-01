/**
 * Daily Ledger Workbench Vue 3 Application
 * Swiss International Typographic Style UI
 */
(function() {
    'use strict';

    const { createApp, ref, reactive, computed, onMounted, watch } = Vue;

    createApp({
        setup() {
            // User state
            const currentUser = ref({});

            // Date & Summary state
            const now = new Date();
            const currentYear = ref(now.getFullYear());
            const currentMonth = ref(now.getMonth() + 1);

            const summary = reactive({
                totalIncome: 0,
                totalExpense: 0,
                netBalance: 0
            });

            // Category state
            const allCategories = ref([]);
            const creatingCategory = ref(false);
            const newCategory = reactive({
                type: 'EXPENSE',
                name: '',
                iconCode: 'tag'
            });

            // Quick entry state
            const isSmartInputMode = ref(false);
            const smartInputText = ref('');
            const submitting = ref(false);

            const getTodayString = () => {
                const d = new Date();
                const year = d.getFullYear();
                const month = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                return `${year}-${month}-${day}`;
            };

            const newRecord = reactive({
                recordType: 'EXPENSE',
                categoryId: null,
                amount: null,
                description: '',
                recordDate: getTodayString()
            });

            // Filter & pagination state
            const showFilters = ref(false);
            const loadingRecords = ref(false);
            const records = ref([]);
            const pageInfo = reactive({
                number: 0,
                size: 15,
                totalPages: 1,
                totalElements: 0
            });

            const filterParams = reactive({
                recordType: '',
                categoryId: null,
                startDate: '',
                endDate: '',
                keyword: ''
            });

            // Edit Modal state
            let editModalInstance = null;
            let categoryModalInstance = null;
            const savingRecord = ref(false);
            const editForm = reactive({
                id: null,
                recordType: 'EXPENSE',
                categoryId: null,
                amount: null,
                description: '',
                recordDate: ''
            });

            // Computed categories
            const filteredCategories = computed(() => {
                return allCategories.value.filter(c => c.type === newRecord.recordType);
            });

            const editFilteredCategories = computed(() => {
                return allCategories.value.filter(c => c.type === editForm.recordType);
            });

            // Watch type switch to auto select first category
            watch(() => newRecord.recordType, () => {
                if (filteredCategories.value.length > 0) {
                    newRecord.categoryId = filteredCategories.value[0].id;
                } else {
                    newRecord.categoryId = null;
                }
            });

            // Helpers
            const formatAmount = (val) => {
                if (val === null || val === undefined || isNaN(val)) return '0.00';
                return Number(val).toLocaleString('zh-TW', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                });
            };

            const setRecordType = (type) => {
                newRecord.recordType = type;
            };

            const setEditRecordType = (type) => {
                editForm.recordType = type;
                if (editFilteredCategories.value.length > 0) {
                    editForm.categoryId = editFilteredCategories.value[0].id;
                }
            };

            // APIs
            const fetchUserProfile = async () => {
                try {
                    const user = await window.http.get('/auth/me');
                    currentUser.value = user;
                } catch (e) {
                    window.location.href = '/login';
                }
            };

            const loadCategories = async () => {
                try {
                    const list = await window.http.get('/categories');
                    allCategories.value = list || [];
                    if (!newRecord.categoryId && filteredCategories.value.length > 0) {
                        newRecord.categoryId = filteredCategories.value[0].id;
                    }
                } catch (err) {
                    console.error('Error loading categories:', err);
                }
            };

            const loadSummary = async () => {
                try {
                    const data = await window.http.get(`/records/summary?year=${currentYear.value}&month=${currentMonth.value}`);
                    if (data) {
                        summary.totalIncome = data.totalIncome || 0;
                        summary.totalExpense = data.totalExpense || 0;
                        summary.netBalance = data.netBalance || 0;
                    }
                } catch (err) {
                    console.error('Error loading summary:', err);
                }
            };

            const loadRecords = async (page = 0) => {
                loadingRecords.value = true;
                try {
                    const params = new URLSearchParams();
                    params.append('page', page);
                    params.append('size', pageInfo.size);

                    if (filterParams.recordType) params.append('recordType', filterParams.recordType);
                    if (filterParams.categoryId) params.append('categoryId', filterParams.categoryId);
                    if (filterParams.startDate) params.append('startDate', filterParams.startDate);
                    if (filterParams.endDate) params.append('endDate', filterParams.endDate);
                    if (filterParams.keyword) params.append('keyword', filterParams.keyword);

                    const data = await window.http.get(`/records?${params.toString()}`);
                    if (data) {
                        records.value = data.content || [];
                        pageInfo.number = data.number;
                        pageInfo.totalPages = data.totalPages;
                        pageInfo.totalElements = data.totalElements;
                    }
                } catch (err) {
                    console.error('Error loading records:', err);
                } finally {
                    loadingRecords.value = false;
                }
            };

            // Month navigation
            const changeMonth = (offset) => {
                let m = currentMonth.value + offset;
                let y = currentYear.value;
                if (m > 12) {
                    m = 1;
                    y++;
                } else if (m < 1) {
                    m = 12;
                    y--;
                }
                currentYear.value = y;
                currentMonth.value = m;

                // Sync filter dates to current selected month
                const start = `${y}-${String(m).padStart(2, '0')}-01`;
                const lastDay = new Date(y, m, 0).getDate();
                const end = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
                filterParams.startDate = start;
                filterParams.endDate = end;

                loadSummary();
                loadRecords(0);
            };

            const resetToCurrentMonth = () => {
                const d = new Date();
                currentYear.value = d.getFullYear();
                currentMonth.value = d.getMonth() + 1;
                filterParams.startDate = '';
                filterParams.endDate = '';
                loadSummary();
                loadRecords(0);
            };

            // Record creation
            const handleCreateRecord = async () => {
                if (!newRecord.categoryId || !newRecord.amount || newRecord.amount <= 0 || !newRecord.recordDate) {
                    window.SwissAlert.error('請填寫完整且有效的記帳資訊 (金額必須大於 0)');
                    return;
                }

                submitting.value = true;
                try {
                    await window.http.post('/records', {
                        recordType: newRecord.recordType,
                        categoryId: newRecord.categoryId,
                        amount: newRecord.amount,
                        description: newRecord.description,
                        recordDate: newRecord.recordDate
                    });

                    window.SwissAlert.toast('記帳成功');
                    newRecord.amount = null;
                    newRecord.description = '';

                    await Promise.all([loadSummary(), loadRecords(pageInfo.number)]);
                } catch (err) {
                    console.error('Create record failed:', err);
                } finally {
                    submitting.value = false;
                }
            };

            const handleQuickSmartRecord = async () => {
                if (!smartInputText.value) {
                    window.SwissAlert.error('請輸入記帳文字');
                    return;
                }

                submitting.value = true;
                try {
                    await window.http.post('/records/quick', {
                        text: smartInputText.value
                    });

                    window.SwissAlert.toast('智能解析入帳成功');
                    smartInputText.value = '';

                    await Promise.all([loadSummary(), loadRecords(0)]);
                } catch (err) {
                    console.error('Quick record failed:', err);
                } finally {
                    submitting.value = false;
                }
            };

            // Edit record
            const openEditModal = (item) => {
                editForm.id = item.id;
                editForm.recordType = item.recordType;
                editForm.categoryId = item.categoryId;
                editForm.amount = item.amount;
                editForm.description = item.description || '';
                editForm.recordDate = item.recordDate;

                if (!editModalInstance) {
                    editModalInstance = new bootstrap.Modal(document.getElementById('editRecordModal'));
                }
                editModalInstance.show();
            };

            const handleUpdateRecord = async () => {
                if (!editForm.categoryId || !editForm.amount || editForm.amount <= 0 || !editForm.recordDate) {
                    window.SwissAlert.error('請填寫完整記帳資料');
                    return;
                }

                savingRecord.value = true;
                try {
                    await window.http.put(`/records/${editForm.id}`, {
                        recordType: editForm.recordType,
                        categoryId: editForm.categoryId,
                        amount: editForm.amount,
                        description: editForm.description,
                        recordDate: editForm.recordDate
                    });

                    window.SwissAlert.toast('紀錄更新成功');
                    if (editModalInstance) editModalInstance.hide();
                    await Promise.all([loadSummary(), loadRecords(pageInfo.number)]);
                } catch (err) {
                    console.error('Update record failed:', err);
                } finally {
                    savingRecord.value = false;
                }
            };

            // Delete record
            const deleteRecord = async (id) => {
                const ok = await window.SwissAlert.confirm(`確定要刪除這筆記帳記錄 (ID: ${id}) 嗎？`, 'DELETE RECORD');
                if (ok) {
                    try {
                        await window.http.delete(`/records/${id}`);
                        window.SwissAlert.toast('記帳記錄已成功刪除');
                        await Promise.all([loadSummary(), loadRecords(pageInfo.number)]);
                    } catch (err) {
                        console.error('Delete record failed:', err);
                    }
                }
            };

            // Category modal
            const openCategoryModal = () => {
                if (!categoryModalInstance) {
                    categoryModalInstance = new bootstrap.Modal(document.getElementById('categoryModal'));
                }
                categoryModalInstance.show();
            };

            const handleCreateCategory = async () => {
                if (!newCategory.name) {
                    window.SwissAlert.error('請輸入分類名稱');
                    return;
                }

                creatingCategory.value = true;
                try {
                    await window.http.post('/categories', {
                        type: newCategory.type,
                        name: newCategory.name,
                        iconCode: newCategory.iconCode,
                        sortOrder: 50
                    });

                    window.SwissAlert.toast('自訂分類建立成功');
                    newCategory.name = '';
                    await loadCategories();
                } catch (err) {
                    console.error('Create category failed:', err);
                } finally {
                    creatingCategory.value = false;
                }
            };

            const deleteCategory = async (id) => {
                const ok = await window.SwissAlert.confirm('確定要刪除此自訂分類嗎？', 'DELETE CATEGORY');
                if (ok) {
                    try {
                        await window.http.delete(`/categories/${id}`);
                        window.SwissAlert.toast('分類已成功刪除');
                        await loadCategories();
                    } catch (err) {
                        console.error('Delete category failed:', err);
                    }
                }
            };

            // Filters & Pagination
            const toggleFilters = () => {
                showFilters.value = !showFilters.value;
            };

            const applyFilters = () => {
                loadRecords(0);
            };

            const resetFilters = () => {
                filterParams.recordType = '';
                filterParams.categoryId = null;
                filterParams.startDate = '';
                filterParams.endDate = '';
                filterParams.keyword = '';
                loadRecords(0);
            };

            const goToPage = (page) => {
                if (page >= 0 && page < pageInfo.totalPages) {
                    loadRecords(page);
                }
            };

            // Logout
            const handleLogout = () => {
                localStorage.removeItem('jwt_token');
                localStorage.removeItem('user_info');
                window.location.href = '/login';
            };

            // Lifecycle
            onMounted(async () => {
                const token = localStorage.getItem('jwt_token');
                if (!token) {
                    window.location.href = '/login';
                    return;
                }

                await fetchUserProfile();
                await loadCategories();
                await Promise.all([loadSummary(), loadRecords(0)]);
            });

            return {
                currentUser,
                currentYear,
                currentMonth,
                summary,
                allCategories,
                filteredCategories,
                editFilteredCategories,
                newCategory,
                creatingCategory,
                isSmartInputMode,
                smartInputText,
                submitting,
                newRecord,
                showFilters,
                loadingRecords,
                records,
                pageInfo,
                filterParams,
                editForm,
                savingRecord,
                formatAmount,
                setRecordType,
                setEditRecordType,
                changeMonth,
                resetToCurrentMonth,
                handleCreateRecord,
                handleQuickSmartRecord,
                openEditModal,
                handleUpdateRecord,
                deleteRecord,
                openCategoryModal,
                handleCreateCategory,
                deleteCategory,
                toggleFilters,
                applyFilters,
                resetFilters,
                goToPage,
                handleLogout
            };
        }
    }).mount('#app');
})();
