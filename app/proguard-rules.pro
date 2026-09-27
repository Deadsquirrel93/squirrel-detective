# ThreatId is stored in Room by enum name() and read back via ThreatId.valueOf() to compute the
# diff between scans, including across app versions. If R8 renames the constants, snapshots
# written by an older build become unreadable, so the constant names must survive minification.
-keep enum com.packagespy.app.domain.model.ThreatId { *; }
