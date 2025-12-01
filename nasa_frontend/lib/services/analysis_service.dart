import '../models/analysis_result.dart';
import '../config/api_config.dart';
import 'api_service.dart';

class AnalysisService {
  // ============================================
  // NASA VERIFIED CORRELATIONS (linkedEvents)
  // ============================================

  static Future<AnalysisResult> getFlareCmeCorrelation() async {
    final data = await ApiService.getMap('${ApiConfig.analysis}/flare-cme');
    return AnalysisResult.fromJson(data);
  }

  static Future<Map<String, dynamic>> getCmeIpsCorrelation() async {
    return await ApiService.getMap('${ApiConfig.analysis}/cme-ips');
  }

  static Future<Map<String, dynamic>> getIpsStormCorrelation() async {
    return await ApiService.getMap('${ApiConfig.analysis}/ips-storm');
  }

  static Future<Map<String, dynamic>> getCompleteChainWithIps() async {
    return await ApiService.getMap('${ApiConfig.analysis}/complete-chain-ips');
  }

  // ============================================
  // MANUAL TEMPORAL CORRELATIONS
  // ============================================

  static Future<Map<String, dynamic>> getFlareCmeCorrelationManual() async {
    return await ApiService.getMap('${ApiConfig.analysis}/manual/flare-cme');
  }

  static Future<Map<String, dynamic>> getCmeIpsCorrelationManual() async {
    return await ApiService.getMap('${ApiConfig.analysis}/manual/cme-ips');
  }

  static Future<Map<String, dynamic>> getIpsStormCorrelationManual() async {
    return await ApiService.getMap('${ApiConfig.analysis}/manual/ips-storm');
  }

  static Future<Map<String, dynamic>> getCompleteChainManual() async {
    return await ApiService.getMap('${ApiConfig.analysis}/manual/complete-chain');
  }

  // ============================================
  // OTHER
  // ============================================

  static Future<Map<String, dynamic>> getDashboardOverview() async {
    return await ApiService.getMap('${ApiConfig.analysis}/dashboard');
  }

  static Future<Map<String, dynamic>> getCompleteChain() async {
    return await ApiService.getMap('${ApiConfig.analysis}/complete-chain');
  }
}