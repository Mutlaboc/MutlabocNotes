package app.homenotes.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ *
 * Инвентарь и экипировка: слоты («кукла») сверху, рюкзак — сеткой ниже.
 * Экран получает состояние и колбэки; данных сам не создаёт.
 * ------------------------------------------------------------------ */

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEquip: (String) -> Unit = {},
    onUnequip: (EquipSlot) -> Unit = {}
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = CozyAuth.Cream,
        topBar = {
            CozyTopBar(title = stringResource(R.string.inventory_title), onBack = onBack)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
        ) {
            when (uiState) {
                InventoryUiState.Loading -> NotesLoadingMessage()
                is InventoryUiState.Error -> NotesStatusMessage(
                    message = uiState.message.asString(),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetry
                )

                is InventoryUiState.Content -> InventoryContent(
                    inventory = uiState.inventory,
                    onEquip = onEquip,
                    onUnequip = onUnequip
                )
            }
        }
    }
}

@Composable
private fun InventoryContent(
    inventory: Inventory,
    onEquip: (String) -> Unit,
    onUnequip: (EquipSlot) -> Unit
) {
    // Диалог предмета: храним id, а сам предмет ищем в актуальном инвентаре,
    // чтобы диалог обновлялся после equip/unequip.
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    val selectedItem = selectedItemId?.let { id -> inventory.items.firstOrNull { it.id == id } }
    if (selectedItem != null) {
        ItemDialog(
            item = selectedItem,
            onEquip = { onEquip(selectedItem.id) },
            onUnequip = { selectedItem.equippedSlot?.let(onUnequip) },
            onDismiss = { selectedItemId = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        InventorySectionLabel(stringResource(R.string.inventory_section_equipment))
        Spacer(Modifier.height(8.dp))
        EquipmentDoll(
            inventory = inventory,
            onSlotClick = { slot ->
                inventory.equippedIn(slot)?.let { selectedItemId = it.id }
            }
        )

        val totals = inventory.totalBonuses()
        if (totals.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TotalBonusesPanel(totals)
        }

        Spacer(Modifier.height(20.dp))
        InventorySectionLabel(stringResource(R.string.inventory_section_backpack))
        Spacer(Modifier.height(8.dp))

        val backpack = inventory.items.filter { !it.isEquipped }
        if (backpack.isEmpty()) {
            Text(
                text = stringResource(R.string.inventory_empty_backpack),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        } else {
            BackpackGrid(items = backpack, onItemClick = { selectedItemId = it.id })
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** «Кукла»: маскот в центре, по три слота слева и справа. */
@Composable
private fun EquipmentDoll(
    inventory: Inventory,
    onSlotClick: (EquipSlot) -> Unit
) {
    PixelPanel(
        fill = CozyAuth.CardCream,
        shadowOffset = 6,
        cornerRadius = 8,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(EquipSlot.HEAD, EquipSlot.BODY, EquipSlot.LEGS).forEach { slot ->
                    EquipSlotTile(slot, inventory.equippedIn(slot), onClick = { onSlotClick(slot) })
                }
            }
            Image(
                bitmap = ImageBitmap.imageResource(id = R.drawable.mascot_stand_01),
                contentDescription = stringResource(R.string.character_mascot_description),
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .size(120.dp)
                    .padding(horizontal = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(EquipSlot.WEAPON, EquipSlot.OFFHAND, EquipSlot.ACCESSORY).forEach { slot ->
                    EquipSlotTile(slot, inventory.equippedIn(slot), onClick = { onSlotClick(slot) })
                }
            }
        }
    }
}

@Composable
private fun EquipSlotTile(
    slot: EquipSlot,
    item: InventoryItem?,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PixelPanel(
            fill = if (item != null) CozyAuth.FieldCream else CozyAuth.Cream,
            border = item?.rarity?.let(::rarityColor) ?: CozyAuth.BrownOutline.copy(alpha = 0.5f),
            shadowOffset = 3,
            cornerRadius = 6
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickable(enabled = item != null) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item?.icon ?: slotPlaceholderIcon(slot),
                    fontSize = 24.sp,
                    color = if (item != null) CozyAuth.Ink else CozyAuth.InkSoft.copy(alpha = 0.35f)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = slotLabel(slot),
            fontFamily = CozyAuth.PixelFont,
            color = CozyAuth.InkSoft,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TotalBonusesPanel(totals: List<ItemBonus>) {
    PixelPanel(
        fill = CozyAuth.FieldCream,
        shadowOffset = 3,
        cornerRadius = 6,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.inventory_bonus_total),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = totals.joinToString(" · ") { bonusText(it) },
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Terracotta,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun BackpackGrid(
    items: List<InventoryItem>,
    onItemClick: (InventoryItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    BackpackTile(item, onClick = { onItemClick(item) }, modifier = Modifier.weight(1f))
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BackpackTile(
    item: InventoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelPanel(
        fill = CozyAuth.CardCream,
        border = rarityColor(item.rarity),
        shadowOffset = 3,
        cornerRadius = 6,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = item.icon, fontSize = 26.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.name,
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Ink,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ItemDialog(
    item: InventoryItem,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CozyAuth.CardCream,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.icon, fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.name,
                        color = CozyAuth.Ink,
                        fontFamily = CozyAuth.PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = rarityLabel(item.rarity),
                        color = rarityColor(item.rarity),
                        fontFamily = CozyAuth.PixelFont,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column {
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        color = CozyAuth.InkSoft,
                        fontFamily = CozyAuth.PixelFont,
                        fontSize = 13.sp
                    )
                }
                if (item.bonuses.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.bonuses.joinToString(" · ") { bonusText(it) },
                        color = CozyAuth.Terracotta,
                        fontFamily = CozyAuth.PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                item.slot?.let { slot ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.inventory_item_slot, slotLabel(slot)),
                        color = CozyAuth.InkSoft,
                        fontFamily = CozyAuth.PixelFont,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            when {
                item.isEquipped -> PixelPrimaryButton(
                    text = stringResource(R.string.inventory_action_unequip),
                    onClick = onUnequip
                )

                item.isEquippable -> PixelPrimaryButton(
                    text = stringResource(R.string.inventory_action_equip),
                    onClick = onEquip
                )
            }
        },
        dismissButton = {
            PixelOutlineButton(text = stringResource(R.string.action_close), onClick = onDismiss)
        }
    )
}

@Composable
private fun InventorySectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = CozyAuth.PixelFont,
        color = CozyAuth.Ink,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
}

/** «+2 Сила» / «-1 Ловкость». */
private fun bonusText(bonus: ItemBonus): String =
    (if (bonus.value >= 0) "+${bonus.value}" else "${bonus.value}") + " " + bonus.statName

/** Цвет редкости — рамка тайлов и подпись в диалоге. */
private fun rarityColor(rarity: ItemRarity): Color = when (rarity) {
    ItemRarity.COMMON -> CozyAuth.BrownOutline
    ItemRarity.UNCOMMON -> CozyAuth.SoftGreen
    ItemRarity.RARE -> Color(0xFF4E88C7)
    ItemRarity.EPIC -> Color(0xFF9A5BB5)
    ItemRarity.LEGENDARY -> CozyAuth.Terracotta
}

@Composable
private fun rarityLabel(rarity: ItemRarity): String = stringResource(
    when (rarity) {
        ItemRarity.COMMON -> R.string.inventory_rarity_common
        ItemRarity.UNCOMMON -> R.string.inventory_rarity_uncommon
        ItemRarity.RARE -> R.string.inventory_rarity_rare
        ItemRarity.EPIC -> R.string.inventory_rarity_epic
        ItemRarity.LEGENDARY -> R.string.inventory_rarity_legendary
    }
)

@Composable
private fun slotLabel(slot: EquipSlot): String = stringResource(
    when (slot) {
        EquipSlot.HEAD -> R.string.inventory_slot_head
        EquipSlot.BODY -> R.string.inventory_slot_body
        EquipSlot.LEGS -> R.string.inventory_slot_legs
        EquipSlot.WEAPON -> R.string.inventory_slot_weapon
        EquipSlot.OFFHAND -> R.string.inventory_slot_offhand
        EquipSlot.ACCESSORY -> R.string.inventory_slot_accessory
    }
)

/** Полупрозрачный глиф пустого слота, пока нет пиксель-арт иконок. */
private fun slotPlaceholderIcon(slot: EquipSlot): String = when (slot) {
    EquipSlot.HEAD -> "⛑"
    EquipSlot.BODY -> "🥋"
    EquipSlot.LEGS -> "👖"
    EquipSlot.WEAPON -> "🗡"
    EquipSlot.OFFHAND -> "🛡"
    EquipSlot.ACCESSORY -> "💍"
}
