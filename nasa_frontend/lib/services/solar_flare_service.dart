import '../config/api_config.dart';
import 'api_service.dart';

class SolarFlareService {
  static Future<Map<String, dynamic>> getStats() async {
    return await ApiService.getMap('${ApiConfig.solarFlares}/stats');
  }
}