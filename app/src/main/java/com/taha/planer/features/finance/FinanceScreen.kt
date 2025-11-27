package com.taha.planer.features.finance

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MILLIS_PER_DAY = 86_400_000L

private const val PREF_FINANCE_TX = "planner_finance_tx"
private const val KEY_FINANCE_TX = "finance_tx_v1"

private const val PREF_FINANCE_DEBT = "planner_finance_debt"
private const val KEY_FINANCE_DEBT = "finance_debt_v1"

private const val PREF_FINANCE_SHOP = "planner_finance_shop"
private const val KEY_FINANCE_SHOP = "finance_shop_v1"

private enum class FinanceTab {
    OVERVIEW, TRANSACTIONS, DEBTS, SHOPPING
}

private enum class TxType(val code: Int, val label: String) {
    INCOME(1, "درآمد"),
    EXPENSE(2, "هزینه"),
    TRANSFER(3, "انتقال");

    companion object {
        fun fromCode(code: Int): TxType =
            values().find { it.code == code } ?: EXPENSE
    }
}

data class FinanceTransaction(
    val id: Long,
    val dayIndex: Int,
    val type: TxType,
    val category: String,
    val amount: Int, // تومان (یا هر واحد)
    val note: String
)

data class DebtItem(
    val id: Long,
    val name: String,
    val totalAmount: Int,
    val remainingAmount: Int,
    val isLent: Boolean, // true یعنی تو پول دادی، false یعنی قرض گرفتی
    val note: String
)

data class ShoppingItem(
    val id: Long,
    val title: String,
    val estimatedAmount: Int,
    val isDone: Boolean
)

@Composable
fun FinanceScreen() {
    val context = LocalContext.current
    var transactions by remember { mutableStateOf(loadFinanceTransactions(context)) }
    var debts by remember { mutableStateOf(loadDebts(context)) }
    var shopping by remember { mutableStateOf(loadShopping(context)) }

    fun persistTx(updated: List<FinanceTransaction>) {
        transactions = updated
        saveFinanceTransactions(context, updated)
    }

    fun persistDebts(updated: List<DebtItem>) {
        debts = updated
        saveDebts(context, updated)
    }

    fun persistShopping(updated: List<ShoppingItem>) {
        shopping = updated
        saveShopping(context, updated)
    }

    var selectedTab by remember { mutableStateOf(FinanceTab.OVERVIEW) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FinanceTabsRow(
            selected = selectedTab,
            onSelect = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            FinanceTab.OVERVIEW -> OverviewTab(
                transactions = transactions,
                debts = debts
            )

            FinanceTab.TRANSACTIONS -> TransactionsTab(
                transactions = transactions,
                onUpdate = { persistTx(it) }
            )

            FinanceTab.DEBTS -> DebtsTab(
                debts = debts,
                onUpdate = { persistDebts(it) }
            )

            FinanceTab.SHOPPING -> ShoppingTab(
                items = shopping,
                onUpdate = { persistShopping(it) }
            )
        }
    }
}

@Composable
private fun FinanceTabsRow(
    selected: FinanceTab,
    onSelect: (FinanceTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SimpleTab(
            text = "نمای کلی",
            selected = selected == FinanceTab.OVERVIEW
        ) { onSelect(FinanceTab.OVERVIEW) }

        SimpleTab(
            text = "تراکنش‌ها",
            selected = selected == FinanceTab.TRANSACTIONS
        ) { onSelect(FinanceTab.TRANSACTIONS) }

        SimpleTab(
            text = "بدهی / طلب",
            selected = selected == FinanceTab.DEBTS
        ) { onSelect(FinanceTab.DEBTS) }

        SimpleTab(
            text = "لیست خرید",
            selected = selected == FinanceTab.SHOPPING
        ) { onSelect(FinanceTab.SHOPPING) }
    }
}

@Composable
private fun SimpleTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------- OVERVIEW ----------------

@Composable
private fun OverviewTab(
    transactions: List<FinanceTransaction>,
    debts: List<DebtItem>
) {
    val todayIndex = currentDayIndex()
    val last30 = transactions.filter { it.dayIndex >= todayIndex - 29 }

    val totalIncome30 = last30.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val totalExpense30 = last30.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val net30 = totalIncome30 - totalExpense30

    val totalLent = debts.filter { it.isLent }.sumOf { it.remainingAmount }
    val totalBorrowed = debts.filter { !it.isLent }.sumOf { it.remainingAmount }

    val last7Indices = (todayIndex - 6..todayIndex).toList()
    val net7 = last7Indices.map { day ->
        val dayTx = transactions.filter { it.dayIndex == day }
        val income = dayTx.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        val expense = dayTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
        income - expense
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "خلاصه‌ی ۳۰ روز اخیر (شبیه ماه مالی)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "درآمد: $totalIncome30   |   هزینه: $totalExpense30   |   خالص: $net30",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "نمودار خالص روزانه ۷ روز اخیر",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                NetLineChart(points = net7)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "وضعیت بدهی / طلب",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مجموع طلب از دیگران: $totalLent\nمجموع بدهی تو به دیگران: $totalBorrowed",
                    style = MaterialTheme.typography.bodySmall
                )
                if (totalLent + totalBorrowed > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val ratioLent =
                        totalLent.toFloat() / (totalLent + totalBorrowed).toFloat()
                    LinearProgressIndicator(
                        progress = ratioLent.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بخش آبی = طلب، بخش باقی = بدهی (فقط یک تخمین دیداری)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun NetLineChart(points: List<Int>) {
    if (points.isEmpty()) {
        Text(
            text = "هنوز برای ۷ روز اخیر تراکنشی ثبت نشده.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val maxAbs = points.maxOf { kotlin.math.abs(it) }
    val safeMax = if (maxAbs <= 0) 1 else maxAbs

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val stepX =
            if (points.size <= 1) size.width
            else size.width / (points.size - 1).toFloat()

        val path = Path()

        points.forEachIndexed { index, value ->
            val ratio = value.toFloat() / safeMax.toFloat() // -1..1
            val centerY = size.height / 2f
            val y = centerY - (ratio * centerY)
            val x = stepX * index
            val p = Offset(x, y)
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }

        drawPath(
            path = path,
            color = MaterialTheme.colorScheme.primary,
            style = Stroke(width = 4f)
        )

        // خط وسط (صفر)
        drawLine(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2f
        )
    }
}

// ---------------- TRANSACTIONS ----------------

@Composable
private fun TransactionsTab(
    transactions: List<FinanceTransaction>,
    onUpdate: (List<FinanceTransaction>) -> Unit
) {
    val todayIndex = currentDayIndex()
    val sorted = transactions.sortedWith(
        compareByDescending<FinanceTransaction> { it.dayIndex }
            .thenByDescending { it.id }
    )

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinanceTransaction?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "تراکنش‌های مالی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "درآمد، هزینه و انتقال‌ها را ثبت کن تا تصویر واضحی از پولت داشته باشی.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(sorted, key = { it.id }) { tx ->
                TransactionRow(
                    tx = tx,
                    todayIndex = todayIndex,
                    onEdit = {
                        editing = tx
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(transactions.filterNot { it.id == tx.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "تراکنش جدید")
            }
        }
    }

    if (showDialog) {
        TransactionDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { newTx ->
                val updated = if (editing == null) {
                    transactions + newTx
                } else {
                    transactions.map { if (it.id == newTx.id) newTx else it }
                }
                onUpdate(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TransactionRow(
    tx: FinanceTransaction,
    todayIndex: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val diff = todayIndex - tx.dayIndex
    val dayLabel = when (diff) {
        0 -> "امروز"
        1 -> "دیروز"
        in 2..7 -> "$diff روز پیش"
        else -> "${diff} روز پیش"
    }

    val sign = when (tx.type) {
        TxType.INCOME -> "+"
        TxType.EXPENSE -> "-"
        TxType.TRANSFER -> "±"
    }

    val color = when (tx.type) {
        TxType.INCOME -> MaterialTheme.colorScheme.primary
        TxType.EXPENSE -> MaterialTheme.colorScheme.error
        TxType.TRANSFER -> MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$dayLabel • ${tx.type.label}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                if (tx.category.isNotBlank()) {
                    Text(
                        text = "دسته: ${tx.category}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (tx.note.isNotBlank()) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$sign${tx.amount}",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "ویرایش")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDialog(
    initial: FinanceTransaction?,
    onDismiss: () -> Unit,
    onSave: (FinanceTransaction) -> Unit
) {
    val todayIndex = currentDayIndex()

    var draftType by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var draftAmount by remember { mutableStateOf(initial?.amount?.toString() ?: "0") }
    var draftCategory by remember { mutableStateOf(initial?.category ?: "") }
    var draftNote by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "تراکنش جدید" else "ویرایش تراکنش")
        },
        text = {
            Column {
                Text(
                    text = "نوع تراکنش:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TxTypeChip(
                        text = TxType.INCOME.label,
                        selected = draftType == TxType.INCOME
                    ) { draftType = TxType.INCOME }

                    TxTypeChip(
                        text = TxType.EXPENSE.label,
                        selected = draftType == TxType.EXPENSE
                    ) { draftType = TxType.EXPENSE }

                    TxTypeChip(
                        text = TxType.TRANSFER.label,
                        selected = draftType == TxType.TRANSFER
                    ) { draftType = TxType.TRANSFER }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = draftAmount,
                    onValueChange = { draftAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مبلغ (عدد)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftCategory,
                    onValueChange = { draftCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("دسته (اختیاری، مثل: غذا، حقوق، شارژ)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    label = { Text("توضیح (اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = draftAmount.toIntOrNull() ?: 0
                    if (amount <= 0) return@TextButton
                    val tx = FinanceTransaction(
                        id = initial?.id ?: System.currentTimeMillis(),
                        dayIndex = initial?.dayIndex ?: todayIndex,
                        type = draftType,
                        category = draftCategory.trim(),
                        amount = amount,
                        note = draftNote.trim()
                    )
                    onSave(tx)
                }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بی‌خیال") }
        }
    )
}

@Composable
private fun TxTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------- DEBTS ----------------

@Composable
private fun DebtsTab(
    debts: List<DebtItem>,
    onUpdate: (List<DebtItem>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DebtItem?>(null) }

    val lent = debts.filter { it.isLent }
    val borrowed = debts.filter { !it.isLent }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "بدهی‌ها و طلب‌ها",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ثبت کن به چه کسی بدهکاری و چه کسی به تو بدهکار است تا چیزی یادت نرود.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "طلب‌ها (دیگران به تو بدهکارند)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(lent, key = { it.id }) { d ->
                DebtRow(
                    debt = d,
                    onPayPartial = { amount ->
                        val remain = (d.remainingAmount - amount).coerceAtLeast(0)
                        val updated = d.copy(remainingAmount = remain)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onSettle = {
                        val updated = d.copy(remainingAmount = 0)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onEdit = {
                        editing = d
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(debts.filterNot { it.id == d.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "بدهی‌ها (تو به دیگران بدهکاری)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(borrowed, key = { it.id }) { d ->
                DebtRow(
                    debt = d,
                    onPayPartial = { amount ->
                        val remain = (d.remainingAmount - amount).coerceAtLeast(0)
                        val updated = d.copy(remainingAmount = remain)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onSettle = {
                        val updated = d.copy(remainingAmount = 0)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onEdit = {
                        editing = d
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(debts.filterNot { it.id == d.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "بدهی/طلب جدید")
            }
        }
    }

    if (showDialog) {
        DebtDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { newDebt ->
                val updated = if (editing == null) {
                    debts + newDebt
                } else {
                    debts.map { if (it.id == newDebt.id) newDebt else it }
                }
                onUpdate(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun DebtRow(
    debt: DebtItem,
    onPayPartial: (Int) -> Unit,
    onSettle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress =
        if (debt.totalAmount > 0)
            (1f - debt.remainingAmount.toFloat() / debt.totalAmount.toFloat()).coerceIn(0f, 1f)
        else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = if (debt.isLent)
                    "طلب از ${debt.name}"
                else
                    "بدهی به ${debt.name}",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "کل: ${debt.totalAmount}   |   باقیمانده: ${debt.remainingAmount}",
                style = MaterialTheme.typography.bodySmall
            )
            if (debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            var showPartialDialog by remember { mutableStateOf(false) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(onClick = { showPartialDialog = true }) {
                    Text("ثبت پرداخت/دریافت")
                }
                TextButton(onClick = onSettle) {
                    Text("تسویه کامل")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                }
            }

            if (showPartialDialog) {
                var draftAmount by remember { mutableStateOf("0") }
                AlertDialog(
                    onDismissRequest = { showPartialDialog = false },
                    title = { Text("مبلغ پرداخت/دریافت") },
                    text = {
                        OutlinedTextField(
                            value = draftAmount,
                            onValueChange = { draftAmount = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("مبلغ (عدد)") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val a = draftAmount.toIntOrNull() ?: 0
                                if (a > 0) {
                                    onPayPartial(a)
                                    showPartialDialog = false
                                }
                            }
                        ) { Text("ثبت") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPartialDialog = false }) {
                            Text("بی‌خیال")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DebtDialog(
    initial: DebtItem?,
    onDismiss: () -> Unit,
    onSave: (DebtItem) -> Unit
) {
    var draftName by remember { mutableStateOf(initial?.name ?: "") }
    var draftTotal by remember { mutableStateOf(initial?.totalAmount?.toString() ?: "0") }
    var draftRemain by remember { mutableStateOf(initial?.remainingAmount?.toString() ?: "0") }
    var draftIsLent by remember { mutableStateOf(initial?.isLent ?: true) }
    var draftNote by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "بدهی/طلب جدید" else "ویرایش بدهی/طلب") },
        text = {
            Column {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام شخص") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = draftTotal,
                        onValueChange = { draftTotal = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("مبلغ کل") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draftRemain,
                        onValueChange = { draftRemain = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("باقیمانده") },
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "نوع:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { draftIsLent = true }) {
                        Text(
                            text = "طلب من از او",
                            fontWeight = if (draftIsLent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    TextButton(onClick = { draftIsLent = false }) {
                        Text(
                            text = "بدهی من به او",
                            fontWeight = if (!draftIsLent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    label = { Text("توضیح (اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = draftName.trim()
                    val total = draftTotal.toIntOrNull() ?: 0
                    val remain = draftRemain.toIntOrNull() ?: 0
                    if (name.isEmpty() || total <= 0) return@TextButton
                    val d = DebtItem(
                        id = initial?.id ?: System.currentTimeMillis(),
                        name = name,
                        totalAmount = total,
                        remainingAmount = remain.coerceIn(0, total),
                        isLent = draftIsLent,
                        note = draftNote.trim()
                    )
                    onSave(d)
                }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بی‌خیال") }
        }
    )
}

// ---------------- SHOPPING ----------------

@Composable
private fun ShoppingTab(
    items: List<ShoppingItem>,
    onUpdate: (List<ShoppingItem>) -> Unit
) {
    val (done, notDone) = items.partition { it.isDone }

    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "لیست خرید",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "هر چیزی می‌خوای بخری اینجا ثبت کن؛ هم برای خرید روزانه، هم خریدهای بزرگ‌تر.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(notDone, key = { it.id }) { item ->
                ShoppingRow(
                    item = item,
                    onToggle = {
                        val updated = item.copy(isDone = !item.isDone)
                        onUpdate(items.map { if (it.id == item.id) updated else it })
                    },
                    onDelete = {
                        onUpdate(items.filterNot { it.id == item.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (done.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "خریدهای انجام‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(done, key = { it.id }) { item ->
                    ShoppingRow(
                        item = item,
                        onToggle = {
                            val updated = item.copy(isDone = !item.isDone)
                            onUpdate(items.map { if (it.id == item.id) updated else it })
                        },
                        onDelete = {
                            onUpdate(items.filterNot { it.id == item.id })
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "آیتم جدید")
            }
        }
    }

    if (showDialog) {
        var draftTitle by remember { mutableStateOf("") }
        var draftAmount by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("آیتم جدید در لیست خرید") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام آیتم (مثلاً: برنج، هدفون، کتاب)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftAmount,
                        onValueChange = { draftAmount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("هزینه تقریبی (اختیاری)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = draftTitle.trim()
                        if (title.isEmpty()) return@TextButton
                        val amount = draftAmount.toIntOrNull() ?: 0
                        val item = ShoppingItem(
                            id = System.currentTimeMillis(),
                            title = title,
                            estimatedAmount = amount,
                            isDone = false
                        )
                        onUpdate(items + item)
                        showDialog = false
                    }
                ) { Text("افزودن") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Bold
                )
                if (item.estimatedAmount > 0) {
                    Text(
                        text = "هزینه تقریبی: ${item.estimatedAmount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (item.isDone) "انجام شد" else "در انتظار خرید",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                FilledTonalButton(onClick = onToggle) {
                    Text(if (item.isDone) "برگردان" else "انجام شد")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                }
            }
        }
    }
}

// ---------------- STORAGE HELPERS ----------------

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun loadFinanceTransactions(context: Context): List<FinanceTransaction> {
    val prefs = context.getSharedPreferences(PREF_FINANCE_TX, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_FINANCE_TX, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
            val typeCode = parts[2].toIntOrNull() ?: TxType.EXPENSE.code
            val category = parts[3]
            val amount = parts[4].toIntOrNull() ?: 0
            val note = parts[5]
            FinanceTransaction(
                id = id,
                dayIndex = dayIndex,
                type = TxTypcat >> app/src/main/java/com/taha/planer/features/finance/FinanceScreen.kt << 'EOF'

// ---------------- DEBTS ----------------

@Composable
private fun DebtsTab(
    debts: List<DebtItem>,
    onUpdate: (List<DebtItem>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DebtItem?>(null) }

    val lent = debts.filter { it.isLent }
    val borrowed = debts.filter { !it.isLent }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "بدهی‌ها و طلب‌ها",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ثبت کن به چه کسی بدهکاری و چه کسی به تو بدهکار است تا چیزی یادت نرود.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "طلب‌ها (دیگران به تو بدهکارند)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(lent, key = { it.id }) { d ->
                DebtRow(
                    debt = d,
                    onPayPartial = { amount ->
                        val remain = (d.remainingAmount - amount).coerceAtLeast(0)
                        val updated = d.copy(remainingAmount = remain)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onSettle = {
                        val updated = d.copy(remainingAmount = 0)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onEdit = {
                        editing = d
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(debts.filterNot { it.id == d.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "بدهی‌ها (تو به دیگران بدهکاری)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(borrowed, key = { it.id }) { d ->
                DebtRow(
                    debt = d,
                    onPayPartial = { amount ->
                        val remain = (d.remainingAmount - amount).coerceAtLeast(0)
                        val updated = d.copy(remainingAmount = remain)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onSettle = {
                        val updated = d.copy(remainingAmount = 0)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onEdit = {
                        editing = d
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(debts.filterNot { it.id == d.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "بدهی/طلب جدید")
            }
        }
    }

    if (showDialog) {
        DebtDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { newDebt ->
                val updated = if (editing == null) {
                    debts + newDebt
                } else {
                    debts.map { if (it.id == newDebt.id) newDebt else it }
                }
                onUpdate(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun DebtRow(
    debt: DebtItem,
    onPayPartial: (Int) -> Unit,
    onSettle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress =
        if (debt.totalAmount > 0)
            (1f - debt.remainingAmount.toFloat() / debt.totalAmount.toFloat()).coerceIn(0f, 1f)
        else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = if (debt.isLent)
                    "طلب از ${debt.name}"
                else
                    "بدهی به ${debt.name}",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "کل: ${debt.totalAmount}   |   باقیمانده: ${debt.remainingAmount}",
                style = MaterialTheme.typography.bodySmall
            )
            if (debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            var showPartialDialog by remember { mutableStateOf(false) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(onClick = { showPartialDialog = true }) {
                    Text("ثبت پرداخت/دریافت")
                }
                TextButton(onClick = onSettle) {
                    Text("تسویه کامل")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                }
            }

            if (showPartialDialog) {
                var draftAmount by remember { mutableStateOf("0") }
                AlertDialog(
                    onDismissRequest = { showPartialDialog = false },
                    title = { Text("مبلغ پرداخت/دریافت") },
                    text = {
                        OutlinedTextField(
                            value = draftAmount,
                            onValueChange = { draftAmount = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("مبلغ (عدد)") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val a = draftAmount.toIntOrNull() ?: 0
                                if (a > 0) {
                                    onPayPartial(a)
                                    showPartialDialog = false
                                }
                            }
                        ) { Text("ثبت") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPartialDialog = false }) {
                            Text("بی‌خیال")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DebtDialog(
    initial: DebtItem?,
    onDismiss: () -> Unit,
    onSave: (DebtItem) -> Unit
) {
    var draftName by remember { mutableStateOf(initial?.name ?: "") }
    var draftTotal by remember { mutableStateOf(initial?.totalAmount?.toString() ?: "0") }
    var draftRemain by remember { mutableStateOf(initial?.remainingAmount?.toString() ?: "0") }
    var draftIsLent by remember { mutableStateOf(initial?.isLent ?: true) }
    var draftNote by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "بدهی/طلب جدید" else "ویرایش بدهی/طلب") },
        text = {
            Column {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام شخص") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = draftTotal,
                        onValueChange = { draftTotal = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("مبلغ کل") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draftRemain,
                        onValueChange = { draftRemain = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("باقیمانده") },
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "نوع:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { draftIsLent = true }) {
                        Text(
                            text = "طلب من از او",
                            fontWeight = if (draftIsLent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    TextButton(onClick = { draftIsLent = false }) {
                        Text(
                            text = "بدهی من به او",
                            fontWeight = if (!draftIsLent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    label = { Text("توضیح (اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = draftName.trim()
                    val total = draftTotal.toIntOrNull() ?: 0
                    val remain = draftRemain.toIntOrNull() ?: 0
                    if (name.isEmpty() || total <= 0) return@TextButton
                    val d = DebtItem(
                        id = initial?.id ?: System.currentTimeMillis(),
                        name = name,
                        totalAmount = total,
                        remainingAmount = remain.coerceIn(0, total),
                        isLent = draftIsLent,
                        note = draftNote.trim()
                    )
                    onSave(d)
                }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بی‌خیال") }
        }
    )
}

// ---------------- DEBTS ----------------

@Composable
private fun DebtsTab(
    debts: List<DebtItem>,
    onUpdate: (List<DebtItem>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DebtItem?>(null) }

    val lent = debts.filter { it.isLent }
    val borrowed = debts.filter { !it.isLent }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "بدهی‌ها و طلب‌ها",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ثبت کن به چه کسی بدهکاری و چه کسی به تو بدهکار است تا چیزی یادت نرود.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "طلب‌ها (دیگران به تو بدهکارند)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(lent, key = { it.id }) { d ->
                DebtRow(
                    debt = d,
                    onPayPartial = { amount ->
                        val remain = (d.remainingAmount - amount).coerceAtLeast(0)
                        val updated = d.copy(remainingAmount = remain)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onSettle = {
                        val updated = d.copy(remainingAmount = 0)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onEdit = {
                        editing = d
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(debts.filterNot { it.id == d.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "بدهی‌ها (تو به دیگران بدهکاری)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(borrowed, key = { it.id }) { d ->
                DebtRow(
                    debt = d,
                    onPayPartial = { amount ->
                        val remain = (d.remainingAmount - amount).coerceAtLeast(0)
                        val updated = d.copy(remainingAmount = remain)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onSettle = {
                        val updated = d.copy(remainingAmount = 0)
                        onUpdate(
                            debts.map { if (it.id == d.id) updated else it }
                        )
                    },
                    onEdit = {
                        editing = d
                        showDialog = true
                    },
                    onDelete = {
                        onUpdate(debts.filterNot { it.id == d.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "بدهی/طلب جدید")
            }
        }
    }

    if (showDialog) {
        DebtDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { newDebt ->
                val updated = if (editing == null) {
                    debts + newDebt
                } else {
                    debts.map { if (it.id == newDebt.id) newDebt else it }
                }
                onUpdate(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun DebtRow(
    debt: DebtItem,
    onPayPartial: (Int) -> Unit,
    onSettle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress =
        if (debt.totalAmount > 0)
            (1f - debt.remainingAmount.toFloat() / debt.totalAmount.toFloat()).coerceIn(0f, 1f)
        else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = if (debt.isLent)
                    "طلب از ${debt.name}"
                else
                    "بدهی به ${debt.name}",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "کل: ${debt.totalAmount}   |   باقیمانده: ${debt.remainingAmount}",
                style = MaterialTheme.typography.bodySmall
            )
            if (debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            var showPartialDialog by remember { mutableStateOf(false) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(onClick = { showPartialDialog = true }) {
                    Text("ثبت پرداخت/دریافت")
                }
                TextButton(onClick = onSettle) {
                    Text("تسویه کامل")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                }
            }

            if (showPartialDialog) {
                var draftAmount by remember { mutableStateOf("0") }
                AlertDialog(
                    onDismissRequest = { showPartialDialog = false },
                    title = { Text("مبلغ پرداخت/دریافت") },
                    text = {
                        OutlinedTextField(
                            value = draftAmount,
                            onValueChange = { draftAmount = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("مبلغ (عدد)") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val a = draftAmount.toIntOrNull() ?: 0
                                if (a > 0) {
                                    onPayPartial(a)
                                    showPartialDialog = false
                                }
                            }
                        ) { Text("ثبت") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPartialDialog = false }) {
                            Text("بی‌خیال")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DebtDialog(
    initial: DebtItem?,
    onDismiss: () -> Unit,
    onSave: (DebtItem) -> Unit
) {
    var draftName by remember { mutableStateOf(initial?.name ?: "") }
    var draftTotal by remember { mutableStateOf(initial?.totalAmount?.toString() ?: "0") }
    var draftRemain by remember { mutableStateOf(initial?.remainingAmount?.toString() ?: "0") }
    var draftIsLent by remember { mutableStateOf(initial?.isLent ?: true) }
    var draftNote by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "بدهی/طلب جدید" else "ویرایش بدهی/طلب") },
        text = {
            Column {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام شخص") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = draftTotal,
                        onValueChange = { draftTotal = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("مبلغ کل") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draftRemain,
                        onValueChange = { draftRemain = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("باقیمانده") },
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "نوع:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { draftIsLent = true }) {
                        Text(
                            text = "طلب من از او",
                            fontWeight = if (draftIsLent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    TextButton(onClick = { draftIsLent = false }) {
                        Text(
                            text = "بدهی من به او",
                            fontWeight = if (!draftIsLent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    label = { Text("توضیح (اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = draftName.trim()
                    val total = draftTotal.toIntOrNull() ?: 0
                    val remain = draftRemain.toIntOrNull() ?: 0
                    if (name.isEmpty() || total <= 0) return@TextButton
                    val d = DebtItem(
                        id = initial?.id ?: System.currentTimeMillis(),
                        name = name,
                        totalAmount = total,
                        remainingAmount = remain.coerceIn(0, total),
                        isLent = draftIsLent,
                        note = draftNote.trim()
                    )
                    onSave(d)
                }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بی‌خیال") }
        }
    )
}

// ---------------- SHOPPING ----------------

@Composable
private fun ShoppingTab(
    items: List<ShoppingItem>,
    onUpdate: (List<ShoppingItem>) -> Unit
) {
    val (done, notDone) = items.partition { it.isDone }

    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "لیست خرید",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "هر چیزی می‌خوای بخری اینجا ثبت کن؛ هم برای خرید روزانه، هم خریدهای بزرگ‌تر.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(notDone, key = { it.id }) { item ->
                ShoppingRow(
                    item = item,
                    onToggle = {
                        val updated = item.copy(isDone = !item.isDone)
                        onUpdate(items.map { if (it.id == item.id) updated else it })
                    },
                    onDelete = {
                        onUpdate(items.filterNot { it.id == item.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (done.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "خریدهای انجام‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(done, key = { it.id }) { item ->
                    ShoppingRow(
                        item = item,
                        onToggle = {
                            val updated = item.copy(isDone = !item.isDone)
                            onUpdate(items.map { if (it.id == item.id) updated else it })
                        },
                        onDelete = {
                            onUpdate(items.filterNot { it.id == item.id })
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "آیتم جدید")
            }
        }
    }

    if (showDialog) {
        var draftTitle by remember { mutableStateOf("") }
        var draftAmount by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("آیتم جدید در لیست خرید") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام آیتم (مثلاً: برنج، هدفون، کتاب)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftAmount,
                        onValueChange = { draftAmount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("هزینه تقریبی (اختیاری)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = draftTitle.trim()
                        if (title.isEmpty()) return@TextButton
                        val amount = draftAmount.toIntOrNull() ?: 0
                        val item = ShoppingItem(
                            id = System.currentTimeMillis(),
                            title = title,
                            estimatedAmount = amount,
                            isDone = false
                        )
                        onUpdate(items + item)
                        showDialog = false
                    }
                ) { Text("افزودن") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Bold
                )
                if (item.estimatedAmount > 0) {
                    Text(
                        text = "هزینه تقریبی: ${item.estimatedAmount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (item.isDone) "انجام شد" else "در انتظار خرید",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                FilledTonalButton(onClick = onToggle) {
                    Text(if (item.isDone) "برگردان" else "انجام شد")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
                }
            }
        }
    }
}

// ---------------- STORAGE HELPERS ----------------

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun loadFinanceTransactions(context: Context): List<FinanceTransaction> {
    val prefs = context.getSharedPreferences(PREF_FINANCE_TX, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_FINANCE_TX, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
            val typeCode = parts[2].toIntOrNull() ?: TxType.EXPENSE.code
            val category = parts[3]
            val amount = parts[4].toIntOrNull() ?: 0
            val note = parts[5]
            FinanceTransaction(
                id = id,
                dayIndex = dayIndex,
                type = TxType.fromCode(typeCode),
                category = category,
                amount = amount,
                note = note
            )
        }
}

private fun saveFinanceTransactions(context: Context, items: List<FinanceTransaction>) {
    val prefs = context.getSharedPreferences(PREF_FINANCE_TX, Context.MODE_PRIVATE)
    val raw = items.joinToString("\n") { t ->
        val safeCat = t.category.replace("\n", " ")
        val safeNote = t.note.replace("\n", " ")
        "${t.id}||${t.dayIndex}||${t.type.code}||$safeCat||${t.amount}||$safeNote"
    }
    prefs.edit().putString(KEY_FINANCE_TX, raw).apply()
}

private fun loadDebts(context: Context): List<DebtItem> {
    val prefs = context.getSharedPreferences(PREF_FINANCE_DEBT, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_FINANCE_DEBT, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val name = parts[1]
            val total = parts[2].toIntOrNull() ?: 0
            val remain = parts[3].toIntOrNull() ?: 0
            val isLent = parts[4] == "1"
            val note = parts[5]
            DebtItem(
                id = id,
                name = name,
                totalAmount = total,
                remainingAmount = remain.coerceIn(0, total),
                isLent = isLent,
                note = note
            )
        }
}

private fun saveDebts(context: Context, items: List<DebtItem>) {
    val prefs = context.getSharedPreferences(PREF_FINANCE_DEBT, Context.MODE_PRIVATE)
    val raw = items.joinToString("\n") { d ->
        val safeName = d.name.replace("\n", " ")
        val safeNote = d.note.replace("\n", " ")
        "${d.id}||$safeName||${d.totalAmount}||${d.remainingAmount}||${if (d.isLent) "1" else "0"}||$safeNote"
    }
    prefs.edit().putString(KEY_FINANCE_DEBT, raw).apply()
}

private fun loadShopping(context: Context): List<ShoppingItem> {
    val prefs = context.getSharedPreferences(PREF_FINANCE_SHOP, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_FINANCE_SHOP, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val amount = parts[2].toIntOrNull() ?: 0
            val done = parts[3] == "1"
            ShoppingItem(
                id = id,
                title = title,
                estimatedAmount = amount,
                isDone = done
            )
        }
}

private fun saveShopping(context: Context, items: List<ShoppingItem>) {
    val prefs = context.getSharedPreferences(PREF_FINANCE_SHOP, Context.MODE_PRIVATE)
    val raw = items.joinToString("\n") { s ->
        val safeTitle = s.title.replace("\n", " ")
        "${s.id}||$safeTitle||${s.estimatedAmount}||${if (s.isDone) "1" else "0"}"
    }
    prefs.edit().putString(KEY_FINANCE_SHOP, raw).apply()
}
