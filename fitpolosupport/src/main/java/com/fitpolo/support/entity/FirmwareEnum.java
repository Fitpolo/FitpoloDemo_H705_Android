package com.fitpolo.support.entity;


public enum FirmwareEnum {
    H703_DFU("0003", 1, "0003_0001_0006_DFU.zip"),
    H705_DFU("0005", 18, "0005_0001_0012_DFU.zip"),;


    private String header;
    private int lastestVersion;
    private String firmwareName;

    FirmwareEnum(String header, int lastestVersion, String firmwareName) {
        this.header = header;
        this.lastestVersion = lastestVersion;
        this.firmwareName = firmwareName;
    }

    public String getHeader() {
        return header;
    }

    public int getLastestVersion() {
        return lastestVersion;
    }

    public String getFirmwareName() {
        return firmwareName;
    }

    public static FirmwareEnum fromHeader(String header) {
        for (FirmwareEnum firwmareEnum : FirmwareEnum.values()) {
            if (firwmareEnum.getHeader().equals(header)) {
                if ("0003".equals(header)) {
                    return H703_DFU;
                }
                if ("0005".equals(header)) {
                    return H705_DFU;
                }
                return firwmareEnum;
            }
        }
        return null;
    }

    public static FirmwareEnum fromLastestVersion(int lastestVersion) {
        for (FirmwareEnum firwmareEnum : FirmwareEnum.values()) {
            if (firwmareEnum.getLastestVersion() == lastestVersion) {
                return firwmareEnum;
            }
        }
        return null;
    }

    public static FirmwareEnum fromFirmwareName(String fromFirmwareName) {
        for (FirmwareEnum firwmareEnum : FirmwareEnum.values()) {
            if (firwmareEnum.getFirmwareName().equals(fromFirmwareName)) {
                return firwmareEnum;
            }
        }
        return null;
    }
}
