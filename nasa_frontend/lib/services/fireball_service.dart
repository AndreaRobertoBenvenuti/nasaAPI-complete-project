import '../models/fireball.dart';
import '../config/api_config.dart';
import 'api_service.dart';

class FireballService {
  static Future<List<Fireball>> getAll() async {
    final data = await ApiService.getList(ApiConfig.fireballs);
    return data.map((json) => Fireball.fromJson(json)).toList();
  }

  static Future<List<Fireball>> getWithLocation() async {
    final data = await ApiService.getList('${ApiConfig.fireballs}/with-location');
    return data.map((json) => Fireball.fromJson(json)).toList();
  }

  static Future<List<Fireball>> getTopEnergy({int limit = 10}) async {
    final data = await ApiService.getList('${ApiConfig.fireballs}/top-energy?limit=$limit');
    return data.map((json) => Fireball.fromJson(json)).toList();
  }

  static Future<Map<String, dynamic>> getStats() async {
    return await ApiService.getMap('${ApiConfig.fireballs}/stats');
  }
}