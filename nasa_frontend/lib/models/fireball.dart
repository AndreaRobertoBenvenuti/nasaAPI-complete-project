class Fireball {
  final int id;
  final DateTime eventDate;
  final double? latitude;
  final double? longitude;
  final double? altitudeKm;
  final double? totalImpactEnergyKt;
  final double? velocityKmS;

  Fireball({
    required this.id,
    required this.eventDate,
    this.latitude,
    this.longitude,
    this.altitudeKm,
    this.totalImpactEnergyKt,
    this.velocityKmS,
  });

  factory Fireball.fromJson(Map<String, dynamic> json) {
    return Fireball(
      id: json['id'] as int,
      eventDate: DateTime.parse(json['eventDate'] as String),
      latitude: json['latitude'] != null ? (json['latitude'] as num).toDouble() : null,
      longitude: json['longitude'] != null ? (json['longitude'] as num).toDouble() : null,
      altitudeKm: json['altitudeKm'] != null ? (json['altitudeKm'] as num).toDouble() : null,
      totalImpactEnergyKt: json['totalImpactEnergyKt'] != null
          ? (json['totalImpactEnergyKt'] as num).toDouble()
          : null,
      velocityKmS: json['velocityKmS'] != null
          ? (json['velocityKmS'] as num).toDouble()
          : null,
    );
  }

  bool get hasLocation => latitude != null && longitude != null;
}