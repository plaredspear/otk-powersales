import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/event.dart';

/// 행사 카드 위젯
///
/// 행사 목록에서 사용되는 카드형 아이템 위젯입니다.
class EventCardWidget extends StatelessWidget {
  final Event event;
  final VoidCallback? onTap;

  const EventCardWidget({
    super.key,
    required this.event,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy.MM.dd');

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 행사 유형 태그
              Chip(
                label: Text(event.eventType),
                backgroundColor: Colors.blue.shade100,
                labelStyle: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),

              // 행사명
              Text(
                event.eventName,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),

              // 행사 기간
              Row(
                children: [
                  const Icon(Icons.calendar_today, size: 16, color: Colors.grey),
                  const SizedBox(width: 4),
                  Text(
                    '${dateFormat.format(event.startDate)} ~ ${dateFormat.format(event.endDate)}',
                    style: const TextStyle(
                      fontSize: 14,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 4),

              // 거래처명
              Row(
                children: [
                  const Icon(Icons.store, size: 16, color: Colors.grey),
                  const SizedBox(width: 4),
                  Text(
                    event.customerName,
                    style: const TextStyle(
                      fontSize: 14,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
