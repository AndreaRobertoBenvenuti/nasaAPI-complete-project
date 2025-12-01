import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../services/analysis_service.dart';
import '../models/analysis_result.dart';
import '../config/app_theme.dart';
import '../widgets/app_drawer.dart';

class AnalysisScreen extends StatefulWidget {
  final int selectedIndex;
  final Function(int) onNavigate;

  const AnalysisScreen({super.key, required this.selectedIndex, required this.onNavigate});

  @override
  State<AnalysisScreen> createState() => _AnalysisScreenState();
}

class _AnalysisScreenState extends State<AnalysisScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  // NASA Verified Data
  bool _isLoadingVerified = false;
  String _errorVerified = '';
  AnalysisResult? _flareCmeVerified;
  Map<String, dynamic>? _cmeIpsVerified;
  Map<String, dynamic>? _ipsStormVerified;
  Map<String, dynamic>? _completeChainVerified;

  // Manual Temporal Data
  bool _isLoadingManual = false;
  String _errorManual = '';
  Map<String, dynamic>? _flareCmeManual;
  Map<String, dynamic>? _cmeIpsManual;
  Map<String, dynamic>? _ipsStormManual;
  Map<String, dynamic>? _completeChainManual;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);

    // Load ONLY NASA Verified data initially (visible tab)
    _loadVerifiedData();

    // Listen for tab changes to load Manual data when needed
    _tabController.addListener(_onTabChanged);
  }

  @override
  void dispose() {
    _tabController.removeListener(_onTabChanged);
    _tabController.dispose();
    super.dispose();
  }

  /// LAZY LOADING: Load Manual data only when user switches to that tab
  void _onTabChanged() {
    if (_tabController.index == 1 && _completeChainManual == null) {
      // User switched to Manual tab and data not loaded yet
      _loadManualData();
    }
  }

  /// Load NASA Verified data (4 API calls)
  Future<void> _loadVerifiedData() async {
    setState(() {
      _isLoadingVerified = true;
      _errorVerified = '';
    });

    try {
      print('📥 Loading NASA Verified data...');

      final flareCmeV = await AnalysisService.getFlareCmeCorrelation();
      final cmeIpsV = await AnalysisService.getCmeIpsCorrelation();
      final ipsStormV = await AnalysisService.getIpsStormCorrelation();
      final chainV = await AnalysisService.getCompleteChainWithIps();

      setState(() {
        _flareCmeVerified = flareCmeV;
        _cmeIpsVerified = cmeIpsV;
        _ipsStormVerified = ipsStormV;
        _completeChainVerified = chainV;
        _isLoadingVerified = false;
      });

      print('✅ NASA Verified data loaded');
    } catch (e) {
      print('❌ Error loading verified data: $e');
      setState(() {
        _errorVerified = e.toString();
        _isLoadingVerified = false;
      });
    }
  }

  /// Load Manual Temporal data (4 API calls) - Called lazily
  Future<void> _loadManualData() async {
    setState(() {
      _isLoadingManual = true;
      _errorManual = '';
    });

    try {
      print('📥 Loading Manual Temporal data...');

      final flareCmeM = await AnalysisService.getFlareCmeCorrelationManual();
      final cmeIpsM = await AnalysisService.getCmeIpsCorrelationManual();
      final ipsStormM = await AnalysisService.getIpsStormCorrelationManual();
      final chainM = await AnalysisService.getCompleteChainManual();

      setState(() {
        _flareCmeManual = flareCmeM;
        _cmeIpsManual = cmeIpsM;
        _ipsStormManual = ipsStormM;
        _completeChainManual = chainM;
        _isLoadingManual = false;
      });

      print('✅ Manual Temporal data loaded');
    } catch (e) {
      print('❌ Error loading manual data: $e');
      setState(() {
        _errorManual = e.toString();
        _isLoadingManual = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: AppDrawer(selectedIndex: widget.selectedIndex, onItemSelected: widget.onNavigate),
      body: Container(
        decoration: const BoxDecoration(gradient: AppTheme.primaryGradient),
        child: SafeArea(
          child: Column(
            children: [
              _buildHeader(),
              _buildTabBar(),
              Expanded(
                child: Container(
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.only(topLeft: Radius.circular(30), topRight: Radius.circular(30)),
                  ),
                  child: TabBarView(
                    controller: _tabController,
                    children: [
                      _buildVerifiedTab(),
                      _buildManualTab(),
                    ],
                  ),
                ),
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
      child: Row(
        children: [
          Builder(
            builder: (context) => IconButton(
              icon: const Icon(Icons.menu, color: Colors.white, size: 28),
              onPressed: () => Scaffold.of(context).openDrawer(),
            ),
          ),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(12)),
            child: const Icon(Icons.analytics, color: Colors.white, size: 32),
          ),
          const SizedBox(width: 16),
          const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Correlation Analysis', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white)),
              Text('Verified vs Temporal', style: TextStyle(fontSize: 14, color: Colors.white70)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTabBar() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.2),
        borderRadius: BorderRadius.circular(16),
      ),
      child: TabBar(
        controller: _tabController,
        indicatorSize: TabBarIndicatorSize.tab, // L'indicatore copre tutto il bottone
        indicator: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
        ),
        labelColor: Colors.blue[700],
        unselectedLabelColor: Colors.white,
        labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
        tabs: const [
          Tab(icon: Icon(Icons.verified, size: 20), text: 'NASA Verified'),
          Tab(icon: Icon(Icons.schedule, size: 20), text: 'Temporal Analysis'),
        ],
      ),
    );
  }

  // ============================================
  // NASA VERIFIED TAB
  // ============================================

  Widget _buildVerifiedTab() {
    if (_isLoadingVerified) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorVerified.isNotEmpty) {
      return _buildError(_errorVerified, _loadVerifiedData);
    }

    if (_flareCmeVerified == null || _cmeIpsVerified == null ||
        _ipsStormVerified == null || _completeChainVerified == null) {
      return const Center(child: CircularProgressIndicator());
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildBadge('NASA DONKI Verified', Colors.blue, Icons.verified_outlined),
          const SizedBox(height: 16),
          _buildCompleteChainCard(
            chains: _completeChainVerified!['completeChains'] as int,
            total: _completeChainVerified!['totalXFlares'] as int,
            color: const Color(0xFF1565C0),
            title: 'NASA Verified Chains',
          ),
          const SizedBox(height: 32),
          _buildSectionTitle('Solar Flare → CME'),
          const SizedBox(height: 16),
          _buildCorrelationCard(
            title: 'NASA Verified Links',
            percentage: _flareCmeVerified!.correlationPercentage,
            total: _flareCmeVerified!.totalMajorFlares,
            correlated: _flareCmeVerified!.flaresWithCme,
            avgDelay: _flareCmeVerified!.averageDelayHours,
            color: Colors.orange,
            icon: Icons.wb_sunny,
          ),
          const SizedBox(height: 16),
          _buildDonutChart(
            _flareCmeVerified!.flaresWithCme,
            _flareCmeVerified!.totalMajorFlares - _flareCmeVerified!.flaresWithCme,
            'With CME',
            'Without CME',
            Colors.orange,
          ),
          const SizedBox(height: 32),
          _buildSectionTitle('CME → Interplanetary Shock'),
          const SizedBox(height: 16),
          _buildCorrelationCard(
            title: 'Satellite Detection',
            percentage: (_cmeIpsVerified?['correlationPercentage'] as num).toDouble(),
            total: _cmeIpsVerified?['totalFastCmes'] as int,
            correlated: _cmeIpsVerified?['cmesWithIps'] as int,
            avgDelay: (_cmeIpsVerified?['averageDelayHours'] as num).toDouble(),
            color: Colors.blue,
            icon: Icons.waves,
          ),
          const SizedBox(height: 16),
          _buildDonutChart(
            _cmeIpsVerified?['cmesWithIps'] as int,
            (_cmeIpsVerified?['totalFastCmes'] as int) - (_cmeIpsVerified?['cmesWithIps'] as int),
            'Detected',
            'Not Detected',
            Colors.blue,
          ),
          const SizedBox(height: 32),
          _buildSectionTitle('Interplanetary Shock → Geomagnetic Storm'),
          const SizedBox(height: 16),
          _buildCorrelationCard(
            title: 'Storm Causation',
            percentage: (_ipsStormVerified?['correlationPercentage'] as num).toDouble(),
            total: _ipsStormVerified?['totalEarthShocks'] as int,
            correlated: _ipsStormVerified?['shocksWithStorm'] as int,
            avgDelay: (_ipsStormVerified?['averageDelayHours'] as num).toDouble(),
            color: Colors.purple,
            icon: Icons.storm,
          ),
          const SizedBox(height: 16),
          _buildDonutChart(
            _ipsStormVerified?['shocksWithStorm'] as int,
            (_ipsStormVerified?['totalEarthShocks'] as int) - (_ipsStormVerified?['shocksWithStorm'] as int),
            'Caused Storm',
            'No Storm',
            Colors.purple,
          ),
          const SizedBox(height: 32),
          _buildScientificNote(
            title: 'NASA DONKI linkedEvents',
            color: Colors.blue,
            items: [
              'Based on NASA\'s scientifically verified linkedEvents data',
              'Events are cross-referenced and validated by DONKI team',
              'Provides the most accurate correlation information',
            ],
          ),
        ],
      ),
    );
  }

  // ============================================
  // MANUAL TEMPORAL TAB
  // ============================================

  Widget _buildManualTab() {
    // Show loading on first access
    if (_completeChainManual == null && !_isLoadingManual && _errorManual.isEmpty) {
      // This shouldn't happen as we load on tab change, but just in case
      return const Center(
        child: Text('Loading...', style: TextStyle(fontSize: 16, color: Colors.grey)),
      );
    }

    if (_isLoadingManual) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorManual.isNotEmpty) {
      return _buildError(_errorManual, _loadManualData);
    }

    if (_flareCmeManual == null || _cmeIpsManual == null ||
        _ipsStormManual == null || _completeChainManual == null) {
      return const Center(child: CircularProgressIndicator());
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildBadge('Temporal Window Analysis', Colors.amber[700]!, Icons.schedule),
          const SizedBox(height: 16),
          _buildCompleteChainCard(
            chains: _completeChainManual!['completeChains'] as int,
            total: _completeChainManual!['totalXFlares'] as int,
            color: Colors.amber[700]!,
            title: 'Temporal Analysis Chains',
          ),
          const SizedBox(height: 32),
          _buildSectionTitle('Solar Flare → CME'),
          const SizedBox(height: 16),
          _buildCorrelationCard(
            title: 'Temporal Correlation (24-60 min)',
            percentage: (_flareCmeManual?['correlationPercentage'] as num).toDouble(),
            total: _flareCmeManual?['totalMajorFlares'] as int,
            correlated: _flareCmeManual?['flaresWithCme'] as int,
            avgDelay: (_flareCmeManual?['averageDelayHours'] as num).toDouble(),
            color: Colors.orange,
            icon: Icons.wb_sunny,
          ),
          const SizedBox(height: 16),
          _buildDonutChart(
            _flareCmeManual?['flaresWithCme'] as int,
            (_flareCmeManual?['totalMajorFlares'] as int) - (_flareCmeManual?['flaresWithCme'] as int),
            'With CME',
            'Without CME',
            Colors.orange,
          ),
          const SizedBox(height: 32),
          _buildSectionTitle('CME → Interplanetary Shock'),
          const SizedBox(height: 16),
          _buildCorrelationCard(
            title: 'Temporal Correlation (15-120h)',
            percentage: (_cmeIpsManual?['correlationPercentage'] as num).toDouble(),
            total: _cmeIpsManual?['totalFastCmes'] as int,
            correlated: _cmeIpsManual?['cmesWithIps'] as int,
            avgDelay: (_cmeIpsManual?['averageDelayHours'] as num).toDouble(),
            color: Colors.blue,
            icon: Icons.waves,
          ),
          const SizedBox(height: 16),
          _buildDonutChart(
            _cmeIpsManual?['cmesWithIps'] as int,
            (_cmeIpsManual?['totalFastCmes'] as int) - (_cmeIpsManual?['cmesWithIps'] as int),
            'Detected',
            'Not Detected',
            Colors.blue,
          ),
          const SizedBox(height: 32),
          _buildSectionTitle('Interplanetary Shock → Geomagnetic Storm'),
          const SizedBox(height: 16),
          _buildCorrelationCard(
            title: 'Temporal Correlation (15-60 min)',
            percentage: (_ipsStormManual?['correlationPercentage'] as num).toDouble(),
            total: _ipsStormManual?['totalEarthShocks'] as int,
            correlated: _ipsStormManual?['shocksWithStorm'] as int,
            avgDelay: (_ipsStormManual?['averageDelayHours'] as num).toDouble(),
            color: Colors.purple,
            icon: Icons.storm,
          ),
          const SizedBox(height: 16),
          _buildDonutChart(
            _ipsStormManual?['shocksWithStorm'] as int,
            (_ipsStormManual?['totalEarthShocks'] as int) - (_ipsStormManual?['shocksWithStorm'] as int),
            'Caused Storm',
            'No Storm',
            Colors.purple,
          ),
          const SizedBox(height: 32),
          _buildScientificNote(
            title: 'Temporal Window Methodology',
            color: Colors.amber[700]!,
            items: [
              'Flare→CME: 24-60 min window (Mahrous et al., 2009)',
              'CME→IPS: 15-120h window, >500 km/s filter (NOAA SWPC)',
              'IPS→Storm: 15-60 min window (L1 satellite data)',
            ],
          ),
        ],
      ),
    );
  }

  // ============================================
  // COMMON WIDGETS
  // ============================================

  Widget _buildError(String error, VoidCallback onRetry) {
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
            Text('Error: $error', textAlign: TextAlign.center),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: onRetry, child: const Text('Retry')),
          ],
        ),
      ),
    );
  }

  Widget _buildBadge(String text, Color color, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color, width: 2),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 18, color: color),
          const SizedBox(width: 8),
          Text(text, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: color)),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(title, style: AppTheme.headlineMedium);
  }

  Widget _buildCompleteChainCard({
    required int chains,
    required int total,
    required Color color,
    required String title,
  }) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: [color, color.withOpacity(0.7)]),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: color.withOpacity(0.4), blurRadius: 20, offset: const Offset(0, 10))],
      ),
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          const Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.wb_sunny, color: Colors.white, size: 28),
              Icon(Icons.arrow_forward, color: Colors.white70, size: 20),
              Icon(Icons.radar, color: Colors.white, size: 28),
              Icon(Icons.arrow_forward, color: Colors.white70, size: 20),
              Icon(Icons.waves, color: Colors.white, size: 28),
              Icon(Icons.arrow_forward, color: Colors.white70, size: 20),
              Icon(Icons.storm, color: Colors.white, size: 28),
            ],
          ),
          const SizedBox(height: 16),
          Text(title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white)),
          const SizedBox(height: 8),
          Text('$chains', style: const TextStyle(fontSize: 48, fontWeight: FontWeight.bold, color: Colors.white)),
          const SizedBox(height: 4),
          Text('From $total X-Class Flares', style: const TextStyle(fontSize: 16, color: Colors.white70)),
          const SizedBox(height: 12),
          const Text('Flare → CME → Shock → Storm', style: TextStyle(fontSize: 14, color: Colors.white70, fontStyle: FontStyle.italic)),
        ],
      ),
    );
  }

  Widget _buildCorrelationCard({
    required String title,
    required double percentage,
    required int total,
    required int correlated,
    required double avgDelay,
    required Color color,
    required IconData icon,
  }) {
    return Container(
      decoration: AppTheme.elevatedCard,
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 28, color: color),
              const SizedBox(width: 12),
              Flexible(
                child: Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
              ),
            ],
          ),
          const SizedBox(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildStatColumn('Correlation', '${percentage.toStringAsFixed(1)}%', color),
              Container(height: 60, width: 1, color: Colors.grey[300]),
              _buildStatColumn('Events', '$correlated / $total', color),
              Container(height: 60, width: 1, color: Colors.grey[300]),
              _buildStatColumn('Avg Delay', '${avgDelay.toStringAsFixed(1)}h', color),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatColumn(String label, String value, Color color) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: color)),
        const SizedBox(height: 4),
        Text(label, style: TextStyle(fontSize: 13, color: Colors.grey[600])),
      ],
    );
  }

  Widget _buildDonutChart(int value1, int value2, String label1, String label2, Color mainColor) {
    return SizedBox(
      height: 220,
      child: Stack(
        children: [
          PieChart(
            PieChartData(
              sections: [
                PieChartSectionData(
                  value: value1.toDouble(),
                  title: '${((value1 / (value1 + value2)) * 100).toStringAsFixed(0)}%',
                  color: mainColor,
                  radius: 80,
                  titleStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                ),
                PieChartSectionData(
                  value: value2.toDouble(),
                  title: '${((value2 / (value1 + value2)) * 100).toStringAsFixed(0)}%',
                  color: Colors.grey[300],
                  radius: 80,
                  titleStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.black54),
                ),
              ],
              centerSpaceRadius: 50,
              sectionsSpace: 3,
            ),
          ),
          Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('${value1 + value2}', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                const Text('Total', style: TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScientificNote({
    required String title,
    required Color color,
    required List<String> items,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.info_outline, color: color, size: 28),
              const SizedBox(width: 12),
              Text(title, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color)),
            ],
          ),
          const SizedBox(height: 16),
          ...items.map((item) => Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _buildValidationItem(item, color),
          )),
        ],
      ),
    );
  }

  Widget _buildValidationItem(String text, Color color) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(Icons.check_circle, color: color, size: 20),
        const SizedBox(width: 8),
        Expanded(child: Text(text, style: const TextStyle(fontSize: 14, height: 1.5))),
      ],
    );
  }
}