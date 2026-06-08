import 'package:flutter/material.dart';

import '../../../domain/entities/claim_code.dart';
import 'claim_form_row.dart';

/// 클레임 요청사항 선택 필드
class ClaimRequestTypeField extends StatelessWidget {
  const ClaimRequestTypeField({
    super.key,
    required this.selectedRequestType,
    required this.requestTypes,
    required this.onRequestTypeSelected,
  });

  final ClaimRequestType? selectedRequestType;
  final List<ClaimRequestType> requestTypes;
  final ValueChanged<ClaimRequestType?> onRequestTypeSelected;

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '요청사항',
      onTap: () => _showRequestTypeSelector(context),
      trailing: const ClaimRowChevron(),
      below: ClaimValueText(
        value: selectedRequestType?.name,
        placeholder: '요청사항 선택',
      ),
    );
  }

  Future<void> _showRequestTypeSelector(BuildContext context) async {
    final selected = await showModalBottomSheet<ClaimRequestType?>(
      context: context,
      builder: (context) => _RequestTypeSelector(
        requestTypes: requestTypes,
        selectedRequestType: selectedRequestType,
      ),
    );

    if (selected != null) {
      onRequestTypeSelected(selected);
    }
  }
}

/// 요청사항 선택 바텀시트
class _RequestTypeSelector extends StatelessWidget {
  const _RequestTypeSelector({
    required this.requestTypes,
    required this.selectedRequestType,
  });

  final List<ClaimRequestType> requestTypes;
  final ClaimRequestType? selectedRequestType;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 헤더
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  '요청사항 선택',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(context, null),
                  child: const Text('선택 해제'),
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          // 요청사항 목록
          Flexible(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: requestTypes.length,
              itemBuilder: (context, index) {
                final requestType = requestTypes[index];
                final isSelected =
                    selectedRequestType?.code == requestType.code;

                return ListTile(
                  title: Text(requestType.name),
                  trailing: isSelected
                      ? const Icon(Icons.check, color: Colors.blue)
                      : null,
                  onTap: () => Navigator.pop(context, requestType),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
