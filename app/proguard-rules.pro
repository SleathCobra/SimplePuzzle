# FragmentManager recreates renderer fragments through their public no-arg constructors.
-keepclassmembers,allowoptimization,allowobfuscation class * extends androidx.fragment.app.Fragment {
    public <init>();
}
