import 'package:flutter/material.dart';

import '../../../domain/entities/claim_code.dart';

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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 필드 라벨 (선택사항)
        const Text(
          '요청사항',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),

        // 요청사항 선택 ListTile
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(4),
            side: BorderSide(color: Colors.grey.shade300),
          ),
          title: Text(
            selectedRequestType?.name ?? '요청사항 선택',
            style: TextStyle(
              fontSize: 14,
              color: selectedRequestType == null
                  ? Colors.grey.shade600
                  : Colors.black87,
            ),
          ),
          trailing: const Icon(Icons.arrow_forward_ios, size: 16),
          onTap: () => _showRequestTypeSelector(context),
        ),
      ],
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
