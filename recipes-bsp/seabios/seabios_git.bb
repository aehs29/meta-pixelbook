DESCRIPTION = "SeaBIOS"
HOMEPAGE = "http://www.coreboot.org/SeaBIOS"
LICENSE = "LGPLv3"
SECTION = "firmware"

SRC_URI = " \
    git://github.com/MrChromebox/SeaBIOS.git;branch=master;protocol=https \
    file://hostcc.patch \
    file://use_python_from_buildsystem.patch \
    file://defconfig \
    file://0001-ldnoexec-Add-script-to-remove-ET_EXEC-flag-from-inte.patch \
    file://remove_gnu_property.patch \
"

# SRCREV = "a18aab36b4b1448112e8446f53d60c5b06ec63b6"
SRCREV = "${AUTOREV}"
PV="1.13.0+git${SRCPV}"

S = "${WORKDIR}/git"

LIC_FILES_CHKSUM = "file://COPYING;md5=d32239bcb673463ab874e80d47fae504         \
                    file://COPYING.LESSER;md5=6a6a8e020838b23406c81b19c1d46df6  \
                    "

FILES_${PN} = "/usr/share/firmware"

DEPENDS = "util-linux-native file-native bison-native flex-native gettext-native acpica-native python3-native"

TUNE_CCARGS = ""
EXTRA_OEMAKE += "HOSTCC='${BUILD_CC}'"
EXTRA_OEMAKE += "CROSS_PREFIX=${TARGET_PREFIX}"

COMPATIBLE_HOST = "(i.86|x86_64).*-linux"

# The CPU variant should be set on the machine.conf
CHROMIUM_CPU_VARIANT ?= "kbl"
BIOSNAME ?= "Meta-Chromebook-MrChromebox-SeaBIOS-${CHROMIUM_CPU_VARIANT}"

SEABIOS_EXTRAVERSION = "${BIOSNAME}-$(date +"%Y.%m.%d")"

PARALLEL_MAKE = ""

do_configure(){
    # Keep the following in case its solved in the repo
    # cp ${S}/configs/.config-${CHROMIUM_CPU_VARIANT}-cros ${S}/.config
    # Use a defconfig that contains new values to make kconf happy
    cp ${WORKDIR}/defconfig ${S}/.config
}

do_compile() {
    unset CPP
    unset CPPFLAGS
    oe_runmake EXTRAVERSION=-${SEABIOS_EXTRAVERSION}
}


do_compile_append (){

    filename="${SEABIOS_EXTRAVERSION}.bin"
    cbfstool ${filename} create -m x86 -s 0x00200000
    cbfstool ${filename} add-payload -f ./out/bios.bin.elf -n payload -b 0x0 -c lzma
    cbfstool ${filename} add -f ./out/vgabios.bin -n vgaroms/seavgabios.bin -t optionrom
    echo "/pci@i0cf8/*@1e,4/drive@0/disk@0\n" > /tmp/bootorder
    cbfstool ${filename} add -f /tmp/bootorder -n bootorder -t raw
    cbfstool ${filename} add-int -i 3000 -n etc/boot-menu-wait
    cbfstool ${filename} print
    md5sum ${filename} > ${filename}.md5
}

do_install() {
    filename="${SEABIOS_EXTRAVERSION}.bin"
    oe_runmake
    install -d ${D}/usr/share/firmware
    install -m 0644 ${filename}* ${D}/${datadir}/firmware/
}


DEPENDS += "coreboot-utils-native make-native python3-native"
