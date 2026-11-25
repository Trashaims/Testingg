package black.com.android.internal.net;

import java.util.List;

import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BField;
import top.niunaijun.blackreflection.annotation.BMethod;

@BClassName("com.android.internal.net.VpnConfig")
public interface VpnConfig {
    @BMethod
    List<String> allowedApplications();

    @BMethod
    List<String> disallowedApplications();

    @BField
    String user();
}
