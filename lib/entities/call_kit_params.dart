import 'package:flutter_callkit_incoming/entities/notification_params.dart';
import 'package:json_annotation/json_annotation.dart';

import 'android_params.dart';
import 'ios_params.dart';

part 'call_kit_params.g.dart';

/// Object config for General.
@JsonSerializable(explicitToJson: true)
class CallKitParams {
  const CallKitParams({
    this.id,
    this.nameCaller,
    this.appName,
    this.avatar,
    this.handle,
    this.type,
    this.normalHandle,
    this.duration,
    this.textAccept,
    this.textDecline,
    this.missedCallNotification,
    this.acceptIcon,
    this.declineIcon,
    this.extra,
    this.headers,
    this.subText,
    this.android,
    this.ios,
    this.appLogo,
    this.onDecline,
  });

  final String? id;
  final String? nameCaller;
  final String? appName;
  final String? avatar;
  final String? declineIcon;
  final String? acceptIcon;
  final String? appLogo;
  final String? handle;
  final int? type;
  final int? normalHandle;
  final int? duration;
  final String? textAccept;
  final String? textDecline;
  final String? subText;
  final NotificationParams? missedCallNotification;
  final Map<String, dynamic>? extra;
  final Map<String, dynamic>? headers;
  final AndroidParams? android;
  final IOSParams? ios;
  @JsonKey(includeFromJson: false, includeToJson: false)
  final Function(String reason)? onDecline;

  factory CallKitParams.fromJson(Map<String, dynamic> json) =>
      _$CallKitParamsFromJson(json);

  Map<String, dynamic> toJson() => _$CallKitParamsToJson(this);
}
