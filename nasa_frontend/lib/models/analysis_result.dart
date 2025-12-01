class AnalysisResult {
  final int totalMajorFlares;
  final int flaresWithCme;
  final double correlationPercentage;
  final double averageDelayHours;

  AnalysisResult({
    required this.totalMajorFlares,
    required this.flaresWithCme,
    required this.correlationPercentage,
    required this.averageDelayHours,
  });

  factory AnalysisResult.fromJson(Map<String, dynamic> json) {
    return AnalysisResult(
      totalMajorFlares: json['totalMajorFlares'] as int,
      flaresWithCme: json['flaresWithCme'] as int,
      correlationPercentage: (json['correlationPercentage'] as num).toDouble(),
      averageDelayHours: (json['averageDelayHours'] as num).toDouble(),
    );
  }
}