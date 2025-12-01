class SolarFlare {
  final int id;
  final DateTime peakTime;
  final String classType;
  final double intensity;
  final String fullClass;

  SolarFlare({
    required this.id,
    required this.peakTime,
    required this.classType,
    required this.intensity,
    required this.fullClass,
  });

  factory SolarFlare.fromJson(Map<String, dynamic> json) {
    return SolarFlare(
      id: json['id'] as int,
      peakTime: DateTime.parse(json['peakTime'] as String),
      classType: json['classType'] as String,
      intensity: (json['intensity'] as num).toDouble(),
      fullClass: json['fullClass'] as String,
    );
  }
}