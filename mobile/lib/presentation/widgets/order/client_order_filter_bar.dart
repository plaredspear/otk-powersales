import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처별 주문 필터 바
///
/// 거래처 선택 드롭다운, 납기일 선택, 검색 버튼으로 구성됩니다.
class ClientOrderFilterBar extends StatelessWidget {
  final Map<int, String> stores; // storeId -> storeName
  final int? selectedStoreId;
  final String selectedDeliveryDate; // YYYY-MM-DD
  final ValueChanged<MapEntry<int, String>?> onStoreChanged; // null = clear
  final ValueChanged<String> onDeliveryDateChanged;
  final VoidCallback onSearch;
  final bool canSearch; // false if no store selected

  const ClientOrderFilterBar({
    super.key,
    required this.stores,
    this.selectedStoreId,
    required this.selectedDeliveryDate,
    required this.onStoreChanged,
    required this.onDeliveryDateChanged,
    required this.onSearch,
    this.canSearch = false,
  });

  @override
  Widget build(BuildContext context) {
    final sortedStores = stores.entries.toList()
      ..sort((a, b) => a.value.compareTo(b.value));

    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          DropdownButtonFormField<int>(
            value: selectedStoreId,
            decoration: const InputDecoration(
              labelText: '거래처 선택',
              border: OutlineInputBorder(),
            ),
            hint: const Text('거래처 선택'),
            items: sortedStores.map((entry) {
              return DropdownMenuItem<int>(
                value: entry.key,
                child: Text(entry.value),
              );
            }).toList(),
            onChanged: (value) {
              if (value == null) {
                onStoreChanged(null);
              } else {
                final storeName = stores[value];
                if (storeName != null) {
                  onStoreChanged(MapEntry(value, storeName));
                }
              }
            },
          ),
          if (!canSearch && selectedStoreId == null) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              '거래처를 선택해주세요',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.error,
              ),
            ),
          ],
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              Expanded(
                child: InkWell(
                  onTap: () async {
                    final pickedDate = await showDatePicker(
                      context: context,
                      initialDate: DateTime.parse(selectedDeliveryDate),
                      firstDate: DateTime(2020),
                      lastDate: DateTime(2030),
                    );
                    if (pickedDate != null) {
                      onDeliveryDateChanged(
                        DateFormat('yyyy-MM-dd').format(pickedDate),
                      );
                    }
                  },
                  child: InputDecorator(
                    decoration: const InputDecoration(
                      labelText: '납기일',
                      border: OutlineInputBorder(),
                      suffixIcon: Icon(Icons.calendar_today),
                    ),
                    child: Text(
                      selectedDeliveryDate,
                      style: AppTypography.bodyMedium,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              ElevatedButton(
                onPressed: canSearch ? onSearch : null,
                child: const Text('검색'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
