import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:fl_chart/fl_chart.dart';
import '../config/app_theme.dart';
import '../services/api_service.dart';
import '../config/api_config.dart';
import '../widgets/app_drawer.dart';

class SolarActivityScreen extends StatefulWidget {
  final int selectedIndex;
  final Function(int) onNavigate;

  const SolarActivityScreen({super.key, required this.selectedIndex, required this.onNavigate});

  @override
  State<SolarActivityScreen> createState() => _SolarActivityScreenState();
}

class _SolarActivityScreenState extends State<SolarActivityScreen> with SingleTickerProviderStateMixin{
  late TabController _tabController;

  bool _isLoading = true;
  String _error = '';

  // Data Lists
  List<dynamic> _cmeEvents = [];
  List<dynamic> _stormEvents = [];
  List<dynamic> _flareEvents = []; // New
  List<dynamic> _ipsEvents = [];   // New

  String _selectedTab = 'cme';

  // Statistics data
  int _totalCme = 0;
  int _fastCme = 0;
  int _totalStorms = 0;
  int _severeStorms = 0;
  int _totalFlares = 0; // New
  int _xClassFlares = 0; // New
  int _totalIps = 0; // New

  // Maps for charts
  Map<int, int> _cmeByMonth = {};
  Map<int, int> _stormsByMonth = {};
  Map<int, int> _flaresByMonth = {};
  Map<int, int> _ipsByMonth = {};
  Map<String, int> _cmeSpeedDistribution = {};
  Map<String, int> _stormKpDistribution = {};
  Map<String, int> _flareClassDistribution = {};

  // Filters and sorting
  String _cmeSortBy = 'date';
  String _cmeFilter = 'all';
  String _cmeTypeFilter = 'all'; // Filtro per Tipo (S, C, O, etc.)
  List<String> _availableCmeTypes = []; // Lista dei tipi trovati
  String _stormSortBy = 'date';
  String _stormFilter = 'all';
  String _flareSortBy = 'date'; // New
  String _flareFilter = 'all';  // New: all, m_class, x_class
  String _ipsSortBy = 'date';   // New
  String _ipsFilter = 'all';    // New: all, earth

  @override
  void initState() {
    super.initState();
    // 5 Tab: CME, Storms, Flares, IPS, Stats
    _tabController = TabController(length: 5, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }


  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });

    try {
      // Assuming ApiConfig has these constants. If not, replace with string paths.
      final cme = await ApiService.getList('${ApiConfig.cme}?limit=50');
      final storms = await ApiService.getList('${ApiConfig.storms}?limit=50');

      // New Endpoints based on your backend structure
      // Note: Backend service uses logic to fetch range, assuming here we fetch a list
      final flares = await ApiService.getList('${ApiConfig.solarFlares}?limit=50');
      final ips = await ApiService.getList('${ApiConfig.ips}?limit=50');

      _calculateStatistics(cme, storms, flares, ips);

      setState(() {
        _cmeEvents = cme;
        _stormEvents = storms;
        _flareEvents = flares;
        _ipsEvents = ips;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  void _calculateStatistics(List<dynamic> cme, List<dynamic> storms, List<dynamic> flares, List<dynamic> ips) {
    // Existing Stats
    _totalCme = cme.length;
    _fastCme = cme.where((e) => ((e['speedKmS'] ?? 0) as num) > 1000).length;
    _totalStorms = storms.length;
    _severeStorms = storms.where((e) => ((e['kpIndex'] ?? 0) as num) >= 7).length;

    // New Stats - Flares
    _totalFlares = flares.length;
    _xClassFlares = flares.where((e) => (e['classType'] ?? '').toString().startsWith('X')).length;

    // New Stats - IPS
    _totalIps = ips.length;

    // Monthly Distribution initialization
    _cmeByMonth = {};
    _stormsByMonth = {};
    _flaresByMonth = {};

    // Helper to populate months
    void populateMonthMap(List<dynamic> list, Map<int, int> map, String dateField) {
      for (var event in list) {
        try {
          if (event[dateField] != null) {
            DateTime date = DateTime.parse(event[dateField]);
            int month = date.month;
            map[month] = (map[month] ?? 0) + 1;
          }
        } catch (e) {}
      }
    }

    populateMonthMap(cme, _cmeByMonth, 'startTime');
    populateMonthMap(storms, _stormsByMonth, 'startTime');
    populateMonthMap(flares, _flaresByMonth, 'peakTime'); // Backend uses peakTime for primary analysis
    populateMonthMap(ips, _ipsByMonth, 'activityTime');

    // CME Speed Dist (Existing)
    _cmeSpeedDistribution = {
      'Slow (<500)': 0, 'Medium (500-1000)': 0, 'Fast (1000-2000)': 0, 'Very Fast (>2000)': 0,
    };
    for (var event in cme) {
      double speed = (event['speedKmS'] ?? 0).toDouble();
      if (speed < 500) _cmeSpeedDistribution['Slow (<500)'] = _cmeSpeedDistribution['Slow (<500)']! + 1;
      else if (speed < 1000) _cmeSpeedDistribution['Medium (500-1000)'] = _cmeSpeedDistribution['Medium (500-1000)']! + 1;
      else if (speed < 2000) _cmeSpeedDistribution['Fast (1000-2000)'] = _cmeSpeedDistribution['Fast (1000-2000)']! + 1;
      else _cmeSpeedDistribution['Very Fast (>2000)'] = _cmeSpeedDistribution['Very Fast (>2000)']! + 1;
    }

    // Estrazione dinamica dei Tipi di CME
    Set<String> types = {};
    for (var event in cme) {
      if (event['type'] != null && event['type'].toString().isNotEmpty) {
        types.add(event['type'].toString());
      }
    }
    _availableCmeTypes = types.toList()..sort();

    // Storm Kp Dist (Existing)
    _stormKpDistribution = {
      'Minor (Kp 5)': 0, 'Moderate (Kp 6)': 0, 'Strong (Kp 7)': 0, 'Severe (Kp 8+)': 0,
    };
    for (var event in storms) {
      int kp = ((event['kpIndex'] ?? 0) as num).toInt();
      if (kp == 5) _stormKpDistribution['Minor (Kp 5)'] = _stormKpDistribution['Minor (Kp 5)']! + 1;
      else if (kp == 6) _stormKpDistribution['Moderate (Kp 6)'] = _stormKpDistribution['Moderate (Kp 6)']! + 1;
      else if (kp == 7) _stormKpDistribution['Strong (Kp 7)'] = _stormKpDistribution['Strong (Kp 7)']! + 1;
      else if (kp >= 8) _stormKpDistribution['Severe (Kp 8+)'] = _stormKpDistribution['Severe (Kp 8+)']! + 1;
    }

    // Flare Class Distribution (New)
    _flareClassDistribution = {
      'A': 0, 'B': 0, 'C': 0, 'M': 0, 'X': 0
    };
    for (var event in flares) {
      String type = (event['classType'] ?? 'A').toString().substring(0, 1);
      if (_flareClassDistribution.containsKey(type)) {
        _flareClassDistribution[type] = _flareClassDistribution[type]! + 1;
      }
    }
  }

  // --- Filter Logic Helper Methods ---

  List<dynamic> _getFilteredAndSortedCME() {
    var filtered = _cmeEvents.where((event) {
      // Filtro per Velocità
      if (_cmeFilter == 'fast') {
        double speed = ((event['speedKmS'] ?? 0) as num).toDouble();
        if (speed <= 1000) return false;
      }

      // Filtro per Tipo
      if (_cmeTypeFilter != 'all') {
        String type = (event['type'] ?? '').toString();
        if (type != _cmeTypeFilter) return false;
      }

      return true;
    }).toList();

    _sortEvents(filtered, _cmeSortBy, 'startTime', numericField: 'speedKmS');
    return filtered;
  }

  List<dynamic> _getFilteredAndSortedStorms() {
    var filtered = _stormEvents.where((event) {
      if (_stormFilter == 'all') return true;
      int kp = ((event['kpIndex'] ?? 0) as num).toInt();
      // filtri per riflettere G-Scale
      if (_stormFilter == 'major') return kp >= 5 && kp <= 6; // G1-G2
      if (_stormFilter == 'severe') return kp >= 7; // G3+
      return true;
    }).toList();

    // Gestione ordinamento G-Scale
    if (_stormSortBy == 'g_scale') {
      // Usiamo KpIndex come proxy numerico per G-Scale
      _sortEvents(filtered, 'kp', 'startTime', numericField: 'kpIndex');
    } else {
      _sortEvents(filtered, _stormSortBy, 'startTime', numericField: 'kpIndex');
    }

    return filtered;
  }

  List<dynamic> _getFilteredAndSortedFlares() {
    var filtered = _flareEvents.where((event) {
      if (_flareFilter == 'all') return true;
      String type = (event['classType'] ?? '').toString();
      if (_flareFilter == 'm_class') return type.startsWith('M');
      if (_flareFilter == 'x_class') return type.startsWith('X');
      return true;
    }).toList();

    // Funzione per calcolare il valore numerico di potenza del flare
    double _getFlareIntensityValue(String classType) {
      if (classType.isEmpty) return 0.0;

      String mainClass = classType.substring(0, 1);
      double subValue = 0.0;

      try {
        // Estrae il numero decimale (es. 5.0 da X5.0)
        String numberPart = classType.substring(1);
        subValue = double.tryParse(numberPart) ?? 0.0;
      } catch (e) {
        // Fallback
        subValue = 0.0;
      }

      // Assegna il valore base alla classe principale
      switch (mainClass) {
        case 'X': return 500.0 + subValue; // X-Class (500 + 5.0 = 505.0)
        case 'M': return 400.0 + subValue; // M-Class (400 + 3.2 = 403.2)
        case 'C': return 300.0 + subValue; // C-Class
        case 'B': return 200.0 + subValue; // B-Class
        case 'A': return 100.0 + subValue; // A-Class
        default: return 0.0;
      }
    }

    // GESTIONE ORDINAMENTO
    if (_flareSortBy == 'intensity') {
      filtered.sort((a, b) {
        String aClass = a['classType'] ?? 'A0.0';
        String bClass = b['classType'] ?? 'A0.0';

        double aVal = _getFlareIntensityValue(aClass);
        double bVal = _getFlareIntensityValue(bClass);

        // Ordina in modo decrescente (più pericoloso prima)
        return bVal.compareTo(aVal);
      });
    } else {
      // Ordinamento di default per data
      _sortEvents(filtered, _flareSortBy, 'peakTime');
    }

    return filtered;
  }

  List<dynamic> _getFilteredAndSortedIPS() {
    var filtered = _ipsEvents.where((event) {
      if (_ipsFilter == 'all') return true;
      String loc = (event['location'] ?? '').toString().toLowerCase();
      if (_ipsFilter == 'earth') return loc.contains('earth');
      return true;
    }).toList();
    _sortEvents(filtered, _ipsSortBy, 'activityTime'); // Backend uses activityTime
    return filtered;
  }

  void _sortEvents(List<dynamic> list, String sortBy, String dateField, {String? numericField}) {
    if (sortBy == 'date') {
      list.sort((a, b) {
        DateTime aDate = DateTime.parse(a[dateField]);
        DateTime bDate = DateTime.parse(b[dateField]);
        return bDate.compareTo(aDate);
      });
    } else if (numericField != null) {
      list.sort((a, b) {
        num aVal = (a[numericField] ?? 0) as num;
        num bVal = (b[numericField] ?? 0) as num;
        return bVal.compareTo(aVal);
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
              _buildStatsOverview(),

              // 1. La nuova TabBar
              _buildTabBar(),

              Expanded(
                child: Container(
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.only(topLeft: Radius.circular(30), topRight: Radius.circular(30)),
                  ),
                  child: _isLoading
                      ? const Center(child: CircularProgressIndicator())
                      : _error.isNotEmpty
                      ? Center(child: Text('Error: $_error'))

                  // 2. La vista controllata dal TabController
                      : _buildTabBarView(),
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
            child: const Icon(Icons.wb_sunny, color: Colors.white, size: 32),
          ),
          const SizedBox(width: 16),
          const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Solar Monitor', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white)),
              Text('CME, Flares & Storms', style: TextStyle(fontSize: 14, color: Colors.white70)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatsOverview() {
    if (_isLoading) return const SizedBox.shrink();
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(16)),
      child: Row(
        children: [
          Expanded(child: _buildStatItem('CME', '$_totalCme', Icons.radar)),
          Container(width: 1, height: 40, color: Colors.white30),
          Expanded(child: _buildStatItem('Storms', '$_totalStorms', Icons.storm)),
          Container(width: 1, height: 40, color: Colors.white30),
          // New Stats
          Expanded(child: _buildStatItem('Flares', '$_totalFlares', Icons.flash_on)),
          Container(width: 1, height: 40, color: Colors.white30),
          Expanded(child: _buildStatItem('IPS', '$_totalIps', Icons.warning_amber_rounded)),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: Colors.white, size: 20),
        const SizedBox(height: 4),
        Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
        Text(label, style: const TextStyle(fontSize: 11, color: Colors.white70), textAlign: TextAlign.center),
      ],
    );
  }

  Widget _buildTabBar() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
      height: 55, // Altezza fissa per contenere bene le tab
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.2),
        borderRadius: BorderRadius.circular(16),
      ),
      child: TabBar(
        controller: _tabController,
        // isScrollable: true, // Necessario perché 5 tab sono troppe per stare fisse
        // tabAlignment: TabAlignment.start, // Allinea a sinistra se scrollabile
        indicatorSize: TabBarIndicatorSize.tab, // L'indicatore copre tutto il bottone
        // dividerColor: Colors.transparent, // Rimuove la linea di default
        // padding: const EdgeInsets.all(4), // Spazio tra il bordo container e le tab

        // Stile dell'indicatore (la pillola bianca)
        indicator: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 4, offset: const Offset(0, 2))
          ],
        ),

        // Colori testo/icone
        labelColor: Colors.blue[700], // Selezionato (Blu scuro)
        unselectedLabelColor: Colors.white, // Non selezionato (Bianco)
        labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),

        tabs: const [
          Tab(text: 'CME', icon: Icon(Icons.radar, size: 20)),
          Tab(text: 'Storms', icon: Icon(Icons.storm, size: 20)),
          Tab(text: 'Flares', icon: Icon(Icons.flash_on, size: 20)),
          Tab(text: 'IPS', icon: Icon(Icons.vibration, size: 20)),
          Tab(text: 'Stats', icon: Icon(Icons.bar_chart, size: 20)),
        ],
      ),
    );
  }

  Widget _buildTab(String label, String value, IconData icon) {
    bool isSelected = _selectedTab == value;
    return GestureDetector(
      onTap: () => setState(() => _selectedTab = value),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
        decoration: BoxDecoration(color: isSelected ? Colors.white : Colors.transparent, borderRadius: BorderRadius.circular(12)),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: isSelected ? AppTheme.primaryBlue : Colors.white, size: 18),
            const SizedBox(width: 6),
            Text(label, style: TextStyle(color: isSelected ? AppTheme.primaryBlue : Colors.white, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal, fontSize: 13)),
          ],
        ),
      ),
    );
  }



  Widget _buildTabBarView() {
    return ClipRRect(
      // Arrotonda il contenuto in alto per seguire il container bianco
      borderRadius: const BorderRadius.only(topLeft: Radius.circular(30), topRight: Radius.circular(30)),
      child: TabBarView(
        controller: _tabController,
        children: [
          _buildCmeList(),
          _buildStormsList(),
          _buildFlaresList(),
          _buildIpsList(),
          _buildStatistics(),
        ],
      ),
    );
  }

  // --- CME & Storms Lists (Slightly Refactored, mostly same) ---

  Widget _buildCmeList() {
    final filteredList = _getFilteredAndSortedCME();
    return Column(
      children: [
        _buildCmeFiltersBar(),
        Expanded(
          child: filteredList.isEmpty
              ? const Center(child: Text('No CME events match the filters'))
              : ListView.builder(
            padding: const EdgeInsets.all(24),
            itemCount: filteredList.length,
            itemBuilder: (context, index) => _buildCmeCard(filteredList[index]),
          ),
        ),
      ],
    );
  }

  Widget _buildStormsList() {
    final filteredList = _getFilteredAndSortedStorms();
    return Column(
      children: [
        _buildStormsFiltersBar(),
        Expanded(
          child: filteredList.isEmpty
              ? const Center(child: Text('No storm events match the filters'))
              : ListView.builder(
            padding: const EdgeInsets.all(24),
            itemCount: filteredList.length,
            itemBuilder: (context, index) => _buildStormCard(filteredList[index]),
          ),
        ),
      ],
    );
  }

  // --- NEW: Flares & IPS Lists ---

  Widget _buildFlaresList() {
    final filteredList = _getFilteredAndSortedFlares();
    return Column(
      children: [
        _buildFlaresFiltersBar(),
        Expanded(
          child: filteredList.isEmpty
              ? const Center(child: Text('No Flare events match the filters'))
              : ListView.builder(
            padding: const EdgeInsets.all(24),
            itemCount: filteredList.length,
            itemBuilder: (context, index) => _buildFlareCard(filteredList[index]),
          ),
        ),
      ],
    );
  }

  Widget _buildIpsList() {
    final filteredList = _getFilteredAndSortedIPS();
    return Column(
      children: [
        _buildIpsFiltersBar(),
        Expanded(
          child: filteredList.isEmpty
              ? const Center(child: Text('No IPS events match the filters'))
              : ListView.builder(
            padding: const EdgeInsets.all(24),
            itemCount: filteredList.length,
            itemBuilder: (context, index) => _buildIpsCard(filteredList[index]),
          ),
        ),
      ],
    );
  }

  // --- Filter Bars ---

  Widget _buildCmeFiltersBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: Colors.grey[100], border: Border(bottom: BorderSide(color: Colors.grey[300]!))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Ordinamento
          Row(
            children: [
              const Icon(Icons.sort, size: 20, color: Colors.grey),
              const SizedBox(width: 8),
              const Text('Sort:', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(width: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  children: [
                    _buildChoiceChip('Date', 'date', _cmeSortBy, (v) => setState(() => _cmeSortBy = v), Colors.red),
                    _buildChoiceChip('Speed', 'speed', _cmeSortBy, (v) => setState(() => _cmeSortBy = v), Colors.red),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // FILTRO TIPO (Mostrato solo se ci sono tipi disponibili)
          if (_availableCmeTypes.isNotEmpty) ...[
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start, // Allinea in alto per gestire wrap
              children: [
                const Padding(
                  padding: EdgeInsets.only(top: 6), // Allineamento visivo con i chip
                  child: Icon(Icons.category, size: 20, color: Colors.grey),
                ),
                const SizedBox(width: 8),
                const Padding(
                  padding: EdgeInsets.only(top: 6),
                  child: Text('Type:', style: TextStyle(fontWeight: FontWeight.w600)),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _buildChoiceChip('All', 'all', _cmeTypeFilter, (v) => setState(() => _cmeTypeFilter = v), Colors.blueGrey),
                        const SizedBox(width: 8),
                        ..._availableCmeTypes.map((type) {
                          return Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: _buildChoiceChip(type, type, _cmeTypeFilter, (v) => setState(() => _cmeTypeFilter = v), Colors.blueGrey),
                          );
                        }),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  // Helper per rendere il codice dei chip più pulito (puoi aggiungerlo alla classe)
  Widget _buildChoiceChip(String label, String value, String currentValue, Function(String) onSelected, Color color) {
    bool isSelected = currentValue == value;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) onSelected(value);
      },
      selectedColor: color,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : Colors.black87,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        fontSize: 12,
      ),
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 0),
      visualDensity: VisualDensity.compact, // Rende i chip più piccoli per farne stare di più
    );
  }

  Widget _buildStormsFiltersBar() {
    return _buildGenericFilterBar(
        sortValue: _stormSortBy,
        onSortChanged: (v) => setState(() => _stormSortBy = v),
        sortOptions: {'date': 'Date', 'kp': 'Kp Index'},
        filterValue: _stormFilter,
        onFilterChanged: (v) => setState(() => _stormFilter = v),
        filterOptions: {'all': 'All', 'major': 'Major (5-6)', 'severe': 'Severe (7+)'},
        color: Colors.purple
    );
  }

  Widget _buildFlaresFiltersBar() {
    return _buildGenericFilterBar(
        sortValue: _flareSortBy,
        onSortChanged: (v) => setState(() => _flareSortBy = v),
        sortOptions: {'date': 'Date', 'intensity': 'Intensity'},
        filterValue: _flareFilter,
        onFilterChanged: (v) => setState(() => _flareFilter = v),
        filterOptions: {'all': 'All', 'm_class': 'M-Class', 'x_class': 'X-Class'},
        color: Colors.orange
    );
  }

  Widget _buildIpsFiltersBar() {
    return _buildGenericFilterBar(
        sortValue: _ipsSortBy,
        onSortChanged: (v) => setState(() => _ipsSortBy = v),
        sortOptions: {'date': 'Date'},
        filterValue: _ipsFilter,
        onFilterChanged: (v) => setState(() => _ipsFilter = v),
        filterOptions: {'all': 'All', 'earth': 'Earth Directed'},
        color: Colors.teal
    );
  }

  Widget _buildGenericFilterBar({
    required String sortValue,
    required Function(String) onSortChanged,
    required Map<String, String> sortOptions,
    required String filterValue,
    required Function(String) onFilterChanged,
    required Map<String, String> filterOptions,
    required Color color
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: Colors.grey[100], border: Border(bottom: BorderSide(color: Colors.grey[300]!))),
      child: Column(
        children: [
          Row(
            children: [
              const Icon(Icons.sort, size: 20, color: Colors.grey),
              const SizedBox(width: 8),
              const Text('Sort by:', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(width: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  children: sortOptions.entries.map((e) => ChoiceChip(
                    label: Text(e.value),
                    selected: sortValue == e.key,
                    onSelected: (s) => s ? onSortChanged(e.key) : null,
                    selectedColor: color,
                    labelStyle: TextStyle(color: sortValue == e.key ? Colors.white : Colors.black87),
                  )).toList(),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              const Icon(Icons.filter_list, size: 20, color: Colors.grey),
              const SizedBox(width: 8),
              const Text('Filter:', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(width: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  children: filterOptions.entries.map((e) => ChoiceChip(
                    label: Text(e.value),
                    selected: filterValue == e.key,
                    onSelected: (s) => s ? onFilterChanged(e.key) : null,
                    selectedColor: color.withOpacity(0.8),
                    labelStyle: TextStyle(color: filterValue == e.key ? Colors.white : Colors.black87),
                  )).toList(),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // --- Cards Implementation ---

  Widget _buildCmeCard(Map<String, dynamic> cme) {
    // Parsing dei dati diretti dalla tua Entity Java
    final date = DateTime.parse(cme['startTime']);
    final speed = ((cme['speedKmS'] ?? 0) as num).toDouble();

    // Ora li prendiamo direttamente dalla root del JSON, come definito nella tua Entity
    final sourceLocation = cme['sourceLocation'];
    final latitude = (cme['latitude'] as num?)?.toDouble();
    final longitude = (cme['longitude'] as num?)?.toDouble();
    final halfAngle = (cme['halfAngleDeg'] as num?)?.toDouble(); // Nota: nella entity si chiama halfAngleDeg


    return _buildGenericCard(
      icon: Icons.radar,
      color: Colors.red,
      title: 'CME Event',
      date: date,
      badge: speed > 1000 ? 'FAST' : null,
      details: [
        // 1. Velocità
        _buildDetailRow(
            Icons.speed,
            'Speed',
            '${speed.toStringAsFixed(1)} km/s',
            color: speed > 1000 ? Colors.red : Colors.orange
        ),

        // 2. Tipo (S, C, etc.)
        if (cme['type'] != null && cme['type'].toString().isNotEmpty)
          _buildDetailRow(Icons.category, 'Type', cme['type'].toString()),

        // 3. Source Location (Punto di origine sul Sole)
        if (sourceLocation != null && sourceLocation.toString().isNotEmpty)
          _buildDetailRow(Icons.wb_sunny, 'Origin', sourceLocation.toString()),

        // 4. Latitudine/Longitudine (Direzione della nube nello spazio)
        if (latitude != null || longitude != null)
          _buildDetailRow(
              Icons.explore,
              'Trajectory',
              'Lat: ${latitude?.toStringAsFixed(1) ?? "?"}°, Lon: ${longitude?.toStringAsFixed(1) ?? "?"}°'
          ),

        // 5. Angolo di apertura (Grandezza della nube)
        if (halfAngle != null)
          _buildDetailRow(Icons.wifi_tethering, 'Half Angle', '${halfAngle.toStringAsFixed(1)}°'),
      ],
      note: cme['note']?.toString(),
    );
  }

  Widget _buildStormCard(Map<String, dynamic> storm) {
    final date = DateTime.parse(storm['startTime']);
    final kp = ((storm['kpIndex'] ?? 0) as num).toInt();

    // Calcoliamo la scala G
    final String gScaleFull = _getGScaleLabel(kp);
    final String gScaleShort = _getShortGScale(kp);

    return _buildGenericCard(
      icon: Icons.storm,
      color: Colors.purple,
      title: 'Geomagnetic Storm',
      date: date,

      // BADGE AGGIORNATO: Mostra G3, G4, etc.
      badge: gScaleShort,
      badgeColor: kp >= 7 ? Colors.red : (kp >= 6 ? Colors.orange : Colors.purple),

      details: [
        // RIGA 1: G-Scale in evidenza
        _buildDetailRow(
            Icons.speed, // Icona tachimetro per l'intensità
            'Scale',
            gScaleFull,
            color: kp >= 7 ? Colors.red : Colors.purple
        ),

        // RIGA 2: Kp Index numerico
        _buildDetailRow(Icons.flash_on, 'Kp Index', '$kp'),

        if (storm['observedTime'] != null)
          _buildDetailRow(Icons.access_time, 'Observed', DateFormat('MMM dd, HH:mm').format(DateTime.parse(storm['observedTime'].toString()))),
      ],
      // Timeline Kp (rimasta uguale)
      customBottom: (storm['allKpIndexes'] is List && storm['allKpIndexes'].isNotEmpty) ?
      Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Kp Timeline:', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: Colors.grey[700])),
            const SizedBox(height: 8),
            Wrap(
              spacing: 6, runSpacing: 6,
              children: (storm['allKpIndexes'] as List).map<Widget>((kpData) {
                int kpVal = ((kpData['kpIndex'] ?? 0) as num).toInt();
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(color: _getKpColor(kpVal).withOpacity(0.2), borderRadius: BorderRadius.circular(6), border: Border.all(color: _getKpColor(kpVal))),
                  child: Text('$kpVal', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: _getKpColor(kpVal))),
                );
              }).toList(),
            ),
          ],
        ),
      ) : null,
    );
  }

  Widget _buildFlareCard(Map<String, dynamic> flare) {
    // Backend service uses: peakTime, fullClass (classType in Flutter map), activeRegionNum
    final date = flare['peakTime'] != null ? DateTime.parse(flare['peakTime']) : DateTime.now();
    final classString = flare['fullClass'] ?? flare['classType'] ?? 'N/A';
    final String type = (classString.toString().isNotEmpty) ? classString.toString()[0] : 'A';
    final region = flare['activeRegionNum'];

    Color flareColor = Colors.yellow[700]!;
    if (type == 'M') flareColor = Colors.orange;
    if (type == 'X') flareColor = Colors.red;

    return _buildGenericCard(
      icon: Icons.flash_on,
      color: flareColor,
      title: 'Solar Flare',
      date: date,
      badge: classString,
      badgeColor: flareColor,
      details: [
        _buildDetailRow(Icons.bar_chart, 'Class', classString.toString(), color: flareColor),
        if (region != null) _buildDetailRow(Icons.map, 'Region', '$region'),
        if (flare['sourceLocation'] != null) _buildDetailRow(Icons.location_searching, 'Location', flare['sourceLocation'].toString()),
        if (flare['beginTime'] != null) _buildDetailRow(Icons.start, 'Begin', DateFormat('HH:mm').format(DateTime.parse(flare['beginTime']))),
        if (flare['endTime'] != null) _buildDetailRow(Icons.stop, 'End', DateFormat('HH:mm').format(DateTime.parse(flare['endTime']))),
      ],
      note: flare['note']?.toString(),
    );
  }

  Widget _buildIpsCard(Map<String, dynamic> ips) {
    // Backend service uses: activityTime, location, catalog
    final date = ips['activityTime'] != null ? DateTime.parse(ips['activityTime']) : DateTime.now();
    final location = ips['location'] ?? 'Unknown';
    bool isEarth = location.toString().toLowerCase().contains('earth');

    return _buildGenericCard(
      icon: Icons.vibration,
      color: Colors.teal,
      title: 'Interplanetary Shock',
      date: date,
      badge: isEarth ? 'EARTH' : null,
      badgeColor: isEarth ? Colors.teal : Colors.grey,
      details: [
        _buildDetailRow(Icons.place, 'Location', location.toString(), color: isEarth ? Colors.teal : Colors.black87),
        if (ips['catalog'] != null) _buildDetailRow(Icons.menu_book, 'Catalog', ips['catalog'].toString()),
        if (ips['instruments'] != null) _buildDetailRow(Icons.satellite, 'Instruments', ips['instruments'].toString()),
      ],
    );
  }

  // --- Generic Card Builder to enforce Style ---

  Widget _buildGenericCard({
    required IconData icon,
    required Color color,
    required String title,
    required DateTime date,
    String? badge,
    Color? badgeColor,
    required List<Widget> details,
    String? note,
    Widget? customBottom,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: AppTheme.cardDecoration,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                  child: Icon(icon, color: color, size: 28),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Text(DateFormat('MMM dd, yyyy - HH:mm').format(date), style: TextStyle(fontSize: 14, color: Colors.grey[600])),
                    ],
                  ),
                ),
                if (badge != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(color: badgeColor ?? color, borderRadius: BorderRadius.circular(12)),
                    child: Text(badge, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(),
            const SizedBox(height: 8),
            ...details,
            if (note != null && note.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: Colors.blue.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(Icons.info_outline, size: 16, color: Colors.blue[700]),
                      const SizedBox(width: 8),
                      Expanded(child: Text(note, style: TextStyle(fontSize: 12, color: Colors.blue[900]))),
                    ],
                  ),
                ),
              ),
            if (customBottom != null) customBottom,
          ],
        ),
      ),
    );
  }

  Color _getKpColor(int kp) {
    if (kp >= 8) return Colors.red;
    if (kp == 7) return Colors.orange;
    if (kp == 6) return Colors.amber;
    return Colors.blue;
  }

  Widget _buildDetailRow(IconData icon, String label, String value, {Color? color}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 16, color: color ?? Colors.grey[600]),
          const SizedBox(width: 8),
          Text('$label: ', style: TextStyle(fontSize: 14, color: Colors.grey[600])),
          Expanded(child: Text(value, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: color))),
        ],
      ),
    );
  }

  // --- Statistics Section ---

  Widget _buildStatistics() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Total Activity by Month', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildMonthlyChart(),
          const SizedBox(height: 32),
          const Text('Flare Class Distribution', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildFlareClassChart(), // New Chart
          const SizedBox(height: 32),
          const Text('CME Speed Distribution', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildCmeSpeedChart(),
          const SizedBox(height: 32),
          const Text('Storm Intensity Distribution', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildStormKpChart(),
        ],
      ),
    );
  }

  Widget _buildMonthlyChart() {
    List<String> months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    double maxVal = 0;
    for (int i = 1; i <= 12; i++) {
      int total = (_cmeByMonth[i] ?? 0) + (_stormsByMonth[i] ?? 0) + (_flaresByMonth[i] ?? 0) + (_ipsByMonth[i] ?? 0);
      if (total > maxVal) maxVal = total.toDouble();
    }
    if (maxVal == 0) maxVal = 10;

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: maxVal * 1.2,
          barTouchData: BarTouchData(
            enabled: true,
            touchTooltipData: BarTouchTooltipData(
              getTooltipItem: (group, groupIndex, rod, rodIndex) {
                int month = group.x + 1;
                int cme = _cmeByMonth[month] ?? 0;
                int storm = _stormsByMonth[month] ?? 0;
                int flare = _flaresByMonth[month] ?? 0;
                int ips = _ipsByMonth[month] ?? 0;
                int total = cme + storm + flare + ips;

                return BarTooltipItem(
                  '${months[group.x]}\n',
                  const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                  children: [
                    TextSpan(
                      text: 'Total: $total\n',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.normal,
                        fontSize: 12,
                      ),
                    ),
                    TextSpan(
                      text: 'CME: $cme\n',
                      style: const TextStyle(
                        color: Colors.red,
                        fontSize: 12,
                      ),
                    ),
                    TextSpan(
                      text: 'Storms: $storm\n',
                      style: const TextStyle(
                        color: Colors.purple,
                        fontSize: 12,
                      ),
                    ),
                    TextSpan(
                      text: 'Flares: $flare\n',
                      style: const TextStyle(
                        color: Colors.orange,
                        fontSize: 12,
                      ),
                    ),
                    TextSpan(
                      text: 'IPS: $ips',
                      style: const TextStyle(
                        color: Colors.teal,
                        fontSize: 12,
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
          barGroups: List.generate(12, (index) {
            int month = index + 1;
            double cme = (_cmeByMonth[month] ?? 0).toDouble();
            double storm = (_stormsByMonth[month] ?? 0).toDouble();
            double flare = (_flaresByMonth[month] ?? 0).toDouble();
            double ips = (_ipsByMonth[month] ?? 0).toDouble();
            return BarChartGroupData(
              x: index,
              barRods: [
                BarChartRodData(
                  toY: cme + storm + flare + ips,
                  width: 14,
                  borderRadius: BorderRadius.zero,
                  rodStackItems: [
                    BarChartRodStackItem(0, cme, Colors.red),
                    BarChartRodStackItem(cme, cme + storm, Colors.purple),
                    BarChartRodStackItem(cme + storm, cme + storm + flare, Colors.orange),
                    BarChartRodStackItem(cme + storm + flare, cme + storm + flare + ips, Colors.teal),
                  ],
                ),
              ],
            );
          }),
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40)),
            bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
              if (value.toInt() >= 0 && value.toInt() < 12) return Text(months[value.toInt()], style: const TextStyle(fontSize: 10));
              return const Text('');
            })),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          gridData: const FlGridData(show: true, drawVerticalLine: false),
          borderData: FlBorderData(show: false),
        ),
      ),
    );
  }

  Widget _buildFlareClassChart() {
    // Classes: A, B, C, M, X
    List<String> classes = ['A', 'B', 'C', 'M', 'X'];
    List<Color> colors = [Colors.grey, Colors.blueGrey, Colors.yellow[700]!, Colors.orange, Colors.red];

    List<BarChartGroupData> bars = [];
    double maxVal = 0;

    for (int i = 0; i < classes.length; i++) {
      double val = (_flareClassDistribution[classes[i]] ?? 0).toDouble();
      if (val > maxVal) maxVal = val;
      bars.add(BarChartGroupData(x: i, barRods: [BarChartRodData(toY: val, color: colors[i], width: 30, borderRadius: BorderRadius.circular(4))]));
    }

    if (maxVal == 0) maxVal = 5;

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: maxVal * 1.2,
          barGroups: bars,
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 30)),
            bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
              if (value.toInt() >= 0 && value.toInt() < classes.length) {
                return Padding(padding: const EdgeInsets.only(top: 8), child: Text(classes[value.toInt()], style: const TextStyle(fontWeight: FontWeight.bold)));
              }
              return const Text('');
            })),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          gridData: const FlGridData(show: false),
          borderData: FlBorderData(show: false),
        ),
      ),
    );
  }

  Widget _buildCmeSpeedChart() {
    List<Color> colors = [Colors.green, Colors.yellow[700]!, Colors.orange, Colors.red];
    int index = 0;
    List<BarChartGroupData> bars = [];
    _cmeSpeedDistribution.forEach((key, value) {
      bars.add(BarChartGroupData(x: index, barRods: [BarChartRodData(toY: value.toDouble(), color: colors[index], width: 40, borderRadius: BorderRadius.circular(8))]));
      index++;
    });
    int maxValue = _cmeSpeedDistribution.values.isEmpty ? 1 : _cmeSpeedDistribution.values.reduce((a, b) => a > b ? a : b);

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: (maxValue * 1.2).toDouble(),
          barGroups: bars,
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40)),
            bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
              List<String> labels = ['Slow', 'Medium', 'Fast', 'V.Fast'];
              if (value.toInt() >= 0 && value.toInt() < labels.length) {
                return Padding(padding: const EdgeInsets.only(top: 8), child: Text(labels[value.toInt()], style: const TextStyle(fontSize: 11)));
              }
              return const Text('');
            })),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          gridData: const FlGridData(show: false),
          borderData: FlBorderData(show: false),
        ),
      ),
    );
  }

  Widget _buildStormKpChart() {
    List<Color> colors = [Colors.blue, Colors.amber, Colors.orange, Colors.red];
    int index = 0;
    List<BarChartGroupData> bars = [];
    _stormKpDistribution.forEach((key, value) {
      bars.add(BarChartGroupData(x: index, barRods: [BarChartRodData(toY: value.toDouble(), color: colors[index], width: 40, borderRadius: BorderRadius.circular(8))]));
      index++;
    });
    int maxValue = _stormKpDistribution.values.isEmpty ? 1 : _stormKpDistribution.values.reduce((a, b) => a > b ? a : b);

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: (maxValue * 1.2).toDouble(),
          barGroups: bars,
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40)),
            bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
              List<String> labels = ['Minor', 'Moderate', 'Strong', 'Severe'];
              if (value.toInt() >= 0 && value.toInt() < labels.length) {
                return Padding(padding: const EdgeInsets.only(top: 8), child: Text(labels[value.toInt()], style: const TextStyle(fontSize: 11)));
              }
              return const Text('');
            })),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          gridData: const FlGridData(show: false),
          borderData: FlBorderData(show: false),
        ),
      ),
    );
  }



  // Helper per convertire Kp in G-Scale (G1-G5)
  String _getGScaleLabel(int kp) {
    if (kp >= 9) return 'G5 (Extreme)';
    if (kp == 8) return 'G4 (Severe)';
    if (kp == 7) return 'G3 (Strong)';
    if (kp == 6) return 'G2 (Moderate)';
    if (kp == 5) return 'G1 (Minor)';
    return 'Sub-G1'; // Sotto il livello di tempesta
  }

  // Helper per ottenere solo il numero G (per badge compatti)
  String _getShortGScale(int kp) {
    if (kp >= 9) return 'G5';
    if (kp >= 5) return 'G${kp - 4}';
    return 'KP$kp';
  }
}