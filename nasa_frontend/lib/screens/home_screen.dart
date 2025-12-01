import 'package:flutter/material.dart';
import '../services/fireball_service.dart';
import '../services/solar_flare_service.dart';
import '../services/analysis_service.dart';
import '../widgets/app_drawer.dart';
import '../widgets/modern_stat_card.dart';
import '../config/app_theme.dart';
import '../widgets/stat_card_with_chart.dart';

class HomeScreen extends StatefulWidget {
  final int selectedIndex;
  final Function(int) onNavigate;

  const HomeScreen({
    super.key,
    required this.selectedIndex,
    required this.onNavigate,
  });

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool _isLoading = true;
  String _error = '';
  Map<String, dynamic>? _fireballStats;
  Map<String, dynamic>? _flareStats;
  Map<String, dynamic>? _dashboardOverview;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });

    try {
      final fireballStats = await FireballService.getStats();
      final flareStats = await SolarFlareService.getStats();
      final dashboardOverview = await AnalysisService.getDashboardOverview();

      setState(() {
        _fireballStats = fireballStats;
        _flareStats = flareStats;
        _dashboardOverview = dashboardOverview;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  @override
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: AppDrawer(
        selectedIndex: widget.selectedIndex,
        onItemSelected: widget.onNavigate,
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildHeader(),
              Expanded(
                child: _isLoading
                    ? const Center(
                  child: CircularProgressIndicator(color: Colors.white),
                )
                    : _error.isNotEmpty
                    ? _buildError()
                    : _buildContent(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Builder(
                builder: (context) => IconButton(
                  icon: const Icon(Icons.menu, color: Colors.white, size: 28),
                  onPressed: () {
                    Scaffold.of(context).openDrawer();
                  },
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.public, color: Colors.white, size: 32),
              ),
              const SizedBox(width: 16),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'NASA Space Events',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      'Real-time data analysis',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.white70,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Container(
        margin: const EdgeInsets.all(24),
        padding: const EdgeInsets.all(24),
        decoration: AppTheme.cardDecoration,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            const Text(
              'Connection Error',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              _error,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.grey),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _loadData,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(30),
          topRight: Radius.circular(30),
        ),
      ),
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Overview',
              style: AppTheme.headlineMedium,
            ),
            const SizedBox(height: 20),

            GridView.count(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisCount: 2,
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 2.5,
              children: [
                ModernStatCard(
                  title: 'Fireballs',
                  value: '${_fireballStats?['total'] ?? 0}',
                  icon: Icons.flash_on,
                  color: Colors.orange,
                  subtitle: 'Detected events',
                ),
                ModernStatCard(
                  title: 'Solar Flares',
                  value: '${_dashboardOverview?['totalFlares'] ?? 0}',
                  icon: Icons.wb_sunny,
                  color: Colors.amber[700]!,
                  subtitle: 'M & X class',
                ),
                ModernStatCard(
                  title: 'CME Events',
                  value: '${_dashboardOverview?['totalCme'] ?? 0}',
                  icon: Icons.radar,
                  color: Colors.red,
                  subtitle: 'Coronal ejections',
                ),
                ModernStatCard(
                  title: 'Storms',
                  value: '${_dashboardOverview?['totalStorms'] ?? 0}',
                  icon: Icons.storm,
                  color: Colors.purple,
                  subtitle: 'Major events',
                ),
              ],
            ),

            const SizedBox(height: 16),

            // IPS Stats Card
            Container(
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF1565C0), Color(0xFF42A5F5)],
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  const Icon(Icons.waves, size: 40, color: Colors.white),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Interplanetary Shocks',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${_dashboardOverview?['earthShocks'] ?? 0} Earth detections from ${_dashboardOverview?['totalIps'] ?? 0} total',
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    '${_dashboardOverview?['totalIps'] ?? 0}',
                    style: const TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 32),

            const Text(
              'Solar Flares Classification',
              style: AppTheme.headlineMedium,
            ),
            const SizedBox(height: 16),

            Container(
              decoration: AppTheme.cardDecoration,
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  _buildFlareBar('X-Class', _flareStats?['classX'] ?? 0,
                      Colors.red, (_flareStats?['total'] ?? 1) as int),
                  const SizedBox(height: 12),
                  _buildFlareBar('M-Class', _flareStats?['classM'] ?? 0,
                      Colors.orange, (_flareStats?['total'] ?? 1) as int),
                  const SizedBox(height: 12),
                  _buildFlareBar('C-Class', _flareStats?['classC'] ?? 0,
                      Colors.yellow[700]!, (_flareStats?['total'] ?? 1) as int),
                  const SizedBox(height: 12),
                  _buildFlareBar('B-Class', _flareStats?['classB'] ?? 0,
                      Colors.blue, (_flareStats?['total'] ?? 1) as int),
                ],
              ),
            ),

            const SizedBox(height: 32),

            const Text(
              'High-Impact Events',
              style: AppTheme.headlineMedium,
            ),
            const SizedBox(height: 16),

            Row(
              children: [
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFFFF6F00), Color(0xFFFF8F00)],
                      ),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      children: [
                        const Icon(Icons.speed, size: 48, color: Colors.white),
                        const SizedBox(height: 12),
                        Text(
                          '${_dashboardOverview?['fastCme'] ?? 0}',
                          style: const TextStyle(
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        const Text(
                          'Fast CME',
                          style: TextStyle(color: Colors.white),
                        ),
                        const Text(
                          '>1000 km/s',
                          style: TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFF6A1B9A), Color(0xFF8E24AA)],
                      ),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      children: [
                        const Icon(Icons.warning_amber, size: 48, color: Colors.white),
                        const SizedBox(height: 12),
                        Text(
                          '${_dashboardOverview?['majorStorms'] ?? 0}',
                          style: const TextStyle(
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        const Text(
                          'Major Storms',
                          style: TextStyle(color: Colors.white),
                        ),
                        const Text(
                          'Kp ≥ 5',
                          style: TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFlareBar(String label, int count, Color color, int total) {
    double percentage = total > 0 ? count / total : 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
              ),
            ),
            Text(
              '$count',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(10),
          child: LinearProgressIndicator(
            value: percentage,
            backgroundColor: Colors.grey[200],
            color: color,
            minHeight: 10,
          ),
        ),
      ],
    );
  }
}