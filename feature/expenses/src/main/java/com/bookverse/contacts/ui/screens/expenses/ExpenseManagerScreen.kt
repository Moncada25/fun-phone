package com.bookverse.contacts.ui.screens.expenses

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.expenses.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import com.bookverse.contacts.ui.components.withoutTop
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.expenses.ExpenseCsv
import com.bookverse.contacts.data.expenses.ExpenseCsvHeaders
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.domain.expenses.ExpenseCurrencyProvider
import com.bookverse.contacts.domain.expenses.ExpenseOverview
import com.bookverse.contacts.domain.expenses.buildBudgetStatuses
import com.bookverse.contacts.domain.expenses.buildCategoryDistribution
import com.bookverse.contacts.domain.expenses.calculateMonthlyTotals
import com.bookverse.contacts.domain.expenses.transactionsForMonth
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.screens.settings.SettingsSelectionSheet
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.chromePolicy
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.ListCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PiggyBank
import com.composables.icons.lucide.Repeat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExpenseSection {
    HOME,
    ADD,
    CATEGORIES,
    BUDGETS,
    FIXED,
    YEARLY,
    SEARCH
}

data class ExpenseNavItem(
    val section: ExpenseSection,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpenseManagerScreen(
    onBackClick: () -> Unit,
    expensesViewModel: ExpensesViewModel,
    appSettingsViewModel: AppSettingsViewModel
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val expenseCurrency by appSettingsViewModel.expenseCurrency.collectAsStateWithLifecycle()
    val categories by expensesViewModel.categories.collectAsStateWithLifecycle()
    val transactions by expensesViewModel.transactions.collectAsStateWithLifecycle()
    val budgets by expensesViewModel.budgets.collectAsStateWithLifecycle()
    val selectedMonth by expensesViewModel.selectedMonth.collectAsStateWithLifecycle()
    val transactionFilter by expensesViewModel.transactionFilter.collectAsStateWithLifecycle()
    val pendingCsvExport by expensesViewModel.pendingCsvExport.collectAsStateWithLifecycle()
    val csvExportResult by expensesViewModel.csvExportResult.collectAsStateWithLifecycle()
    val sensitiveLockEnabled by appSettingsViewModel.sensitiveSectionsLockEnabled.collectAsStateWithLifecycle()
    val protectedSessionUnlocked by appSettingsViewModel.protectedSessionUnlocked.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    var currentSection by rememberSaveable { mutableStateOf(ExpenseSection.HOME) }
    var editingTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingTransaction = transactions.firstOrNull { it.id == editingTransactionId }
    var transactionReturnSection by rememberSaveable { mutableStateOf(ExpenseSection.HOME) }
    var linkedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isFixedMode by rememberSaveable { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()

    val handleBack = {
        if (currentSection == ExpenseSection.HOME) {
            onBackClick()
        } else {
            currentSection = ExpenseSection.HOME
            editingTransactionId = null
            transactionReturnSection = ExpenseSection.HOME
            linkedCategoryId = null
            isFixedMode = false
        }
    }

    BackHandler(onBack = handleBack)

    val fixedTransactions = remember(transactions) { transactions.filter { it.isFixed } }
    val monthlyTransactions = remember(transactions, selectedMonth) {
        transactionsForMonth(transactions, selectedMonth)
    }
    val monthlyTotals =
        remember(monthlyTransactions) { calculateMonthlyTotals(monthlyTransactions) }
    val monthlyDistribution =
        remember(monthlyTransactions) { buildCategoryDistribution(monthlyTransactions) }
    val budgetStatuses = remember(monthlyTransactions, budgets) {
        buildBudgetStatuses(monthlyTransactions, budgets)
    }
    val categoryUsage = remember(transactions) {
        transactions
            .mapNotNull { it.categoryId }
            .groupingBy { it }
            .eachCount()
    }
    val recentTransactions = remember(monthlyTransactions) {
        monthlyTransactions.sortedWith(
            compareByDescending<ExpenseTransaction> { it.date }.thenByDescending { it.createdAt }
        )
    }
    val overviewData = remember(
        selectedMonth,
        monthlyTotals,
        monthlyDistribution,
        recentTransactions,
        budgetStatuses
    ) {
        ExpenseOverview(
            month = selectedMonth,
            totals = monthlyTotals,
            distribution = monthlyDistribution,
            recentTransactions = recentTransactions,
            budgets = budgetStatuses
        )
    }

    val navItems = remember {
        listOf(
            ExpenseNavItem(ExpenseSection.HOME, R.string.expense_nav_home, Lucide.PiggyBank),
            ExpenseNavItem(ExpenseSection.BUDGETS, R.string.expense_nav_budgets, Icons.Default.MonetizationOn)
        )
    }
    val sectionPageIds = remember(navItems) { navItems.map { it.section } }
    val initialSectionPage = sectionPageIds.indexOf(currentSection).coerceAtLeast(0)
    val sectionPagerState = rememberPagerState(initialPage = initialSectionPage) {
        sectionPageIds.size
    }
    val focusedSection by remember(sectionPageIds) {
        derivedStateOf {
            val indicatorProgress =
                sectionPagerState.currentPage + sectionPagerState.currentPageOffsetFraction
            val focusedPage = (indicatorProgress + 0.5f)
                .toInt()
                .coerceIn(sectionPageIds.indices)
            sectionPageIds[focusedPage]
        }
    }
    val selectedSection = focusedSection.takeIf { currentSection in sectionPageIds }
    var pagerReady by remember { mutableStateOf(false) }

    LaunchedEffect(sectionPageIds, currentSection) {
        val targetPage = sectionPageIds.indexOf(currentSection)
        if (targetPage == -1) {
            pagerReady = false
            return@LaunchedEffect
        }
        pagerReady = false
        if (sectionPagerState.currentPage != targetPage) {
            sectionPagerState.animateScrollToPage(targetPage)
        }
        pagerReady = true
    }
    LaunchedEffect(
        sectionPagerState.currentPage,
        sectionPagerState.isScrollInProgress,
        sectionPageIds,
        pagerReady
    ) {
        if (!pagerReady || sectionPagerState.isScrollInProgress || currentSection !in sectionPageIds) {
            return@LaunchedEffect
        }
        sectionPageIds.getOrNull(sectionPagerState.currentPage)?.let { section ->
            if (section != currentSection) {
                currentSection = section
                editingTransactionId = null
                isFixedMode = false
            }
        }
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = LocaleController.effectiveLocale(context)
    val currencyOptions = remember(locale) { ExpenseCurrencyProvider.availableOptions(locale) }

    val coroutineScope = rememberCoroutineScope()
    val exportDateFormatter = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = expensesViewModel.exportFixedTransactions(uri).await()
                result.fold(onSuccess = { summary ->
                    Toast.makeText(
                        context,
                        resources.getQuantityString(
                            R.plurals.expense_fixed_export_success,
                            summary.count,
                            summary.count,
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }, onFailure = {
                    DiagnosticLog.error(DiagnosticEvent.EXPENSE_EXPORT_FAILED)
                    Toast.makeText(
                        context,
                        resources.getString(R.string.expense_fixed_export_error),
                        Toast.LENGTH_LONG
                    ).show()
                })
            }
        }
    }

    val csvHeaders = ExpenseCsvHeaders(
        date = stringResource(R.string.expense_date_label),
        type = stringResource(R.string.expense_filter_type_label),
        amount = stringResource(R.string.expense_amount_label),
        currency = stringResource(R.string.expense_currency_settings),
        category = stringResource(R.string.expense_category_label),
        note = stringResource(R.string.expense_note_label),
        recurring = stringResource(R.string.expense_transaction_recurring_badge),
    )
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExpenseCsv.MIME_TYPE)
    ) { uri ->
        if (uri != null) expensesViewModel.queueCsvExport(uri)
    }
    LaunchedEffect(pendingCsvExport, sensitiveLockEnabled, protectedSessionUnlocked) {
        if (pendingCsvExport == null) return@LaunchedEffect
        val locked = appSettingsViewModel.sensitiveSectionsLockEnabled.value &&
            !appSettingsViewModel.protectedSessionUnlocked.value
        if (!locked) {
            expensesViewModel.runPendingCsvExport(csvHeaders, expenseCurrency.code)
        }
    }
    LaunchedEffect(csvExportResult) {
        val result = csvExportResult ?: return@LaunchedEffect
        expensesViewModel.consumeCsvExportResult()
        result.fold(onSuccess = { summary ->
            Toast.makeText(
                context,
                resources.getQuantityString(
                    R.plurals.expense_csv_export_success,
                    summary.count,
                    summary.count,
                ),
                Toast.LENGTH_LONG
            ).show()
        }, onFailure = {
            DiagnosticLog.error(DiagnosticEvent.EXPENSE_EXPORT_FAILED)
            Toast.makeText(
                context,
                resources.getString(R.string.expense_csv_export_error),
                Toast.LENGTH_LONG
            ).show()
        })
    }
    LaunchedEffect(currentSection) {
        if (currentSection == ExpenseSection.SEARCH && !expensesViewModel.transactionFilter.value.isActive) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = expensesViewModel.importFixedTransactions(uri).await()
                result.fold(onSuccess = { summary ->
                    Toast.makeText(
                        context,
                        resources.getQuantityString(
                            R.plurals.expense_fixed_import_success,
                            summary.count,
                            summary.count,
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }, onFailure = { error ->
                    DiagnosticLog.error(DiagnosticEvent.EXPENSE_IMPORT_FAILED)
                    val messageRes = if (error is IllegalArgumentException) {
                        R.string.expense_fixed_import_error_invalid
                    } else {
                        R.string.expense_fixed_import_error
                    }
                    Toast.makeText(
                        context,
                        resources.getString(messageRes),
                        Toast.LENGTH_LONG
                    ).show()
                })
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.expense_manager_title),
                onBackClick = handleBack,
                actions = {
                    IconAction(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(CoreUiR.string.more_options),
                        onClick = { showManageDialog = true },
                        feedback = true
                    )
                },
                scrollBehavior = scrollBehavior,
                titleContent = if (currentSection == ExpenseSection.SEARCH) {
                    {
                        TopBarSearchField(
                            value = transactionFilter.query,
                            onValueChange = {
                                expensesViewModel.setTransactionFilter(transactionFilter.copy(query = it))
                            },
                            placeholder = stringResource(R.string.expense_search_placeholder),
                            focusRequester = searchFocusRequester,
                            onClose = handleBack,
                            clearContentDescription = stringResource(CoreUiR.string.clear),
                        )
                    }
                } else null
            )
        },
        topBarFooter = {
            val segmentedColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                inactiveContentColor = MaterialTheme.colorScheme.onSurface
            )
            val navScroll = rememberScrollState()
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(navScroll)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                navItems.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = item.section == selectedSection,
                        onClick = {
                            currentSection = item.section
                            editingTransactionId = null
                            if (item.section == ExpenseSection.ADD) {
                                isFixedMode = false
                                transactionReturnSection = ExpenseSection.HOME
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, navItems.size),
                        colors = segmentedColors,
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                                Text(
                                    text = stringResource(item.labelRes),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    )
                }
            }

        },
        floatingActionButton = {
            if (currentSection == ExpenseSection.HOME) {
                val isMinimalFab = accentBarStyle.chromePolicy.neutralControls
                val fabContainerColor =
                    if (isMinimalFab) MaterialTheme.colorScheme.surface else accentColor
                val fabContentColor =
                    if (isMinimalFab) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground
                val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                val onFabClick = {
                    editingTransactionId = null
                    transactionReturnSection = ExpenseSection.HOME
                    currentSection = ExpenseSection.ADD
                }
                if (accentBarStyle == com.bookverse.contacts.ui.theme.AccentBarStyle.MODERN) {
                    com.bookverse.contacts.ui.components.ModernFloatingActionButton(
                        icon = Icons.Default.AttachMoney,
                        contentDescription = stringResource(R.string.expense_add_transaction),
                        onClick = onFabClick,
                    )
                } else if (isMinimalFab) {
                    Surface(
                        onClick = onFabClick,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = fabContainerColor,
                        contentColor = fabContentColor,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, outlineColor)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AttachMoney,
                                contentDescription = stringResource(R.string.expense_add_transaction)
                            )
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = onFabClick,
                        containerColor = fabContainerColor,
                        contentColor = fabContentColor,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = stringResource(R.string.expense_add_transaction)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val modern = accentBarStyle == com.bookverse.contacts.ui.theme.AccentBarStyle.MODERN
        val contentTopPadding = if (modern) paddingValues.calculateTopPadding() else 0.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (modern) paddingValues.withoutTop() else paddingValues)
        ) {
            val contentState = if (currentSection in sectionPageIds) {
                ExpenseSection.HOME
            } else {
                currentSection
            }
            AnimatedContent(
                targetState = contentState,
                modifier = Modifier.weight(1f).padding(top = if (currentSection in sectionPageIds) 0.dp else contentTopPadding),
                label = "expense_section"
            ) { section ->
                if (section in sectionPageIds) {
                    HorizontalPager(
                        state = sectionPagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (sectionPageIds[page]) {
                            ExpenseSection.HOME -> ExpenseHomeScreen(
                                overview = overviewData,
                                transactions = overviewData.recentTransactions,
                                accentColor = accentColor,
                                currency = expenseCurrency,
                                month = selectedMonth,
                                onPrevMonth = { expensesViewModel.decrementMonth(1) },
                                onNextMonth = { expensesViewModel.incrementMonth(1) },
                                onEditTransaction = {
                                    editingTransactionId = it.id
                                    isFixedMode = it.isFixed
                                    transactionReturnSection = ExpenseSection.HOME
                                    currentSection = ExpenseSection.ADD
                                },
                                onDeleteTransaction = {
                                    expensesViewModel.deleteTransaction(it.id)
                                },
                                modifier = Modifier.fillMaxSize(),
                                topContentPadding = contentTopPadding,
                            )

                            ExpenseSection.BUDGETS -> ExpenseBudgetsScreen(
                                budgets = budgets,
                                statuses = overviewData.budgets,
                                categories = categories,
                                accentColor = accentColor,
                                currency = expenseCurrency,
                                onSaveBudget = {
                                    expensesViewModel.saveBudget(
                                        id = it.id,
                                        categoryId = it.categoryId,
                                        limit = it.limit
                                    )
                                },
                                onDeleteBudget = { expensesViewModel.deleteBudget(it.id) },
                                modifier = Modifier.padding(top = contentTopPadding)
                            )

                            else -> Unit
                        }
                    }
                } else when (section) {
                        ExpenseSection.ADD -> AddTransactionScreen(
                            transaction = editingTransaction,
                            categories = categories,
                            accentColor = accentColor,
                            currency = expenseCurrency,
                            isFixedMode = isFixedMode,
                            onSave = { type, amount, category, date, note, fixed ->
                                expensesViewModel.saveTransaction(
                                    id = editingTransaction?.id ?: 0,
                                    type = type,
                                    amount = amount,
                                    category = category,
                                    date = date,
                                    note = note,
                                    isFixed = fixed
                                )
                                editingTransactionId = null
                                isFixedMode = false
                                currentSection = transactionReturnSection
                            },
                            onDelete = if (editingTransaction != null) {
                                {
                                    expensesViewModel.deleteTransaction(editingTransaction.id)
                                    editingTransactionId = null
                                    isFixedMode = false
                                    currentSection = transactionReturnSection
                                }
                            } else null,
                            onCancel = {
                                editingTransactionId = null
                                isFixedMode = false
                                currentSection = transactionReturnSection
                            },
                            appSettingsViewModel = appSettingsViewModel
                        )

                        ExpenseSection.CATEGORIES -> ExpenseCategoriesScreen(
                            categories = categories,
                            transactions = transactions,
                            accentColor = accentColor,
                            currency = expenseCurrency,
                            categoryUsage = categoryUsage,
                            linkedCategoryId = linkedCategoryId,
                            onLinkedCategoryChange = { linkedCategoryId = it },
                            onSaveCategory = { category ->
                                expensesViewModel.saveCategory(
                                    id = category.id,
                                    name = category.name,
                                    colorHex = category.colorHex,
                                    isDefault = category.isDefault,
                                    type = category.type,
                                    nameResKey = category.nameResKey
                                )
                            },
                            onDeleteCategory = { category, onResult ->
                                expensesViewModel.deleteCategory(category.id, onResult)
                            },
                            onEditTransaction = {
                                editingTransactionId = it.id
                                isFixedMode = it.isFixed
                                transactionReturnSection = ExpenseSection.CATEGORIES
                                currentSection = ExpenseSection.ADD
                            },
                            onDeleteTransaction = { expensesViewModel.deleteTransaction(it.id) },
                        )

                        ExpenseSection.FIXED -> FixedTransactionsScreen(
                            transactions = fixedTransactions,
                            accentColor = accentColor,
                            currency = expenseCurrency,
                            onAddTransaction = {
                                editingTransactionId = null
                                isFixedMode = true
                                transactionReturnSection = ExpenseSection.FIXED
                                currentSection = ExpenseSection.ADD
                            },
                            onEditTransaction = {
                                editingTransactionId = it.id
                                isFixedMode = true
                                transactionReturnSection = ExpenseSection.FIXED
                                currentSection = ExpenseSection.ADD
                            },
                            onDeleteTransaction = { expensesViewModel.deleteTransaction(it.id) },
                            onExport = {
                                if (fixedTransactions.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.expense_fixed_export_empty),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val fileName =
                                        "fixed_transactions_${exportDateFormatter.format(Date())}.json"
                                    exportLauncher.launch(fileName)
                                }
                            },
                            onImport = {
                                importLauncher.launch(
                                    arrayOf("application/json", "application/octet-stream")
                                )
                            },
                        )

                        ExpenseSection.YEARLY -> ExpenseYearlyScreen(
                            transactions = transactions,
                            initialYear = selectedMonth.year,
                            accentColor = accentColor,
                            currency = expenseCurrency,
                        )

                        ExpenseSection.SEARCH -> ExpenseSearchScreen(
                            transactions = transactions,
                            categories = categories,
                            filter = transactionFilter,
                            onFilterChange = expensesViewModel::setTransactionFilter,
                            onClearFilters = expensesViewModel::clearTransactionFilter,
                            accentColor = accentColor,
                            currency = expenseCurrency,
                            onEditTransaction = {
                                editingTransactionId = it.id
                                isFixedMode = it.isFixed
                                transactionReturnSection = ExpenseSection.SEARCH
                                currentSection = ExpenseSection.ADD
                            },
                            onDeleteTransaction = { expensesViewModel.deleteTransaction(it.id) },
                        )

                        else -> Unit
                }
            }
    }
    }

    if (showManageDialog) {
        val actionItems = listOf(
            OptionsDialogItem(
                label = stringResource(R.string.expense_add_transaction),
                icon = Icons.Default.AttachMoney,
                onClick = {
                    editingTransactionId = null
                    isFixedMode = false
                    transactionReturnSection = ExpenseSection.HOME
                    currentSection = ExpenseSection.ADD
                }
            ),
            OptionsDialogItem(
                label = stringResource(R.string.expense_search_title),
                icon = Icons.Default.Search,
                onClick = {
                    currentSection = ExpenseSection.SEARCH
                }
            ),
            OptionsDialogItem(
                label = stringResource(R.string.expense_yearly_title),
                icon = Icons.Default.BarChart,
                onClick = {
                    currentSection = ExpenseSection.YEARLY
                }
            ),
            OptionsDialogItem(
                label = stringResource(R.string.expense_fixed_transactions_title),
                icon = Lucide.Repeat,
                onClick = {
                    currentSection = ExpenseSection.FIXED
                }
            ),
            OptionsDialogItem(
                label = stringResource(R.string.expense_nav_categories),
                icon = Lucide.ListCheck,
                onClick = {
                    currentSection = ExpenseSection.CATEGORIES
                }
            ),
            OptionsDialogItem(
                label = stringResource(R.string.expense_csv_export_action),
                icon = Icons.Default.FileDownload,
                onClick = {
                    if (transactions.isEmpty()) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.expense_csv_export_empty),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        csvExportLauncher.launch(
                            "transactions_${exportDateFormatter.format(Date())}.csv"
                        )
                    }
                }
            ),
            OptionsDialogItem(
                label = stringResource(R.string.expense_currency_settings),
                icon = Lucide.Coins,
                onClick = {
                    showManageDialog = false
                    showCurrencyDialog = true
                }
            )
        )
        OptionsDialog(
            title = stringResource(CoreUiR.string.more_options),
            options = actionItems,
            onDismiss = { showManageDialog = false },
        )
    }

    if (showCurrencyDialog) {
        SettingsSelectionSheet(
            title = stringResource(R.string.expense_currency_dialog_title),
            options = currencyOptions,
            selectedOption = currencyOptions.firstOrNull { it.code == expenseCurrency.code },
            onSelected = { option ->
                appSettingsViewModel.setExpenseCurrency(context, option.code)
            },
            onDismiss = { showCurrencyDialog = false },
            optionKey = { option -> option.code },
            optionLabel = { option -> "${option.name} (${option.code})" },
        )
    }
}
