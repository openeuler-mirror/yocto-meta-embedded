SUMMARY = "User space tools for kernel auditing"
DESCRIPTION = "The audit package contains the user space utilities for \
storing and searching the audit records generated by the audit subsystem \
in the Linux kernel."
HOMEPAGE = "http://people.redhat.com/sgrubb/audit/"
SECTION = "base"
LICENSE = "GPLv2+ & LGPLv2+"

#inherit autotools python3native update-rc.d systemd
inherit autotools

LIC_FILES_CHKSUM = "file://COPYING;md5=94d55d512a9ba36caa9b7df079bae19f"
SRC_URI = "file://audit/audit-${PV}.tar.gz \
           file://auditd.conf \
           file://audit.rules \
          "


UPDATERCPN = "auditd"
INITSCRIPT_NAME = "auditd"
INITSCRIPT_PARAMS = "defaults"

SYSTEMD_PACKAGES = "auditd"
SYSTEMD_SERVICE_auditd = "auditd.service"

#DEPENDS += "libcap-ng linux-libc-headers libpam"
DEPENDS += "libcap-ng libpam"

EXTRA_OECONF += " --enable-gssapi-krb5=no \
        --with-libcap-ng=yes \
        --with-python3=no \
        --libdir=${base_libdir} \
        --sbindir=${base_sbindir} \
        --without-python \
        --without-golang \
        --disable-zos-remote \
        --with-arm=yes \
        --with-aarch64=yes \
        "

EXTRA_OECONF_append += "${@  "  --build=x86_64-linux --host=${HOST_ARCH}-euler-linux --target=${HOST_ARCH}-euler-linux " if "${TCLIBC}"=="musl" else "" }"

EXTRA_OEMAKE += " \
	STDINC='${STAGING_INCDIR}' \
	"

UPDATERCD = ""

SUMMARY_audispd-plugins = "Plugins for the audit event dispatcher"
DESCRIPTION_audispd-plugins = "The audispd-plugins package provides plugins for the real-time \
interface to the audit system, audispd. These plugins can do things \
like relay events to remote machines or analyze events for suspicious \
behavior."

PACKAGES =+ "audispd-plugins"
PACKAGES += "auditd"

FILES_${PN} = "${sysconfdir}/libaudit.conf ${base_libdir}/libaudit.so.1* ${base_libdir}/libauparse.so.*"
FILES_auditd += "${bindir}/* ${base_sbindir}/* ${sysconfdir}/* ${datadir}/audit/*"
FILES_audispd-plugins += "${sysconfdir}/audit/audisp-remote.conf \
	${sysconfdir}/audit/plugins.d/au-remote.conf \
	${sbindir}/audisp-remote ${localstatedir}/spool/audit \
	"
FILES_${PN}-dbg += "${libdir}/python${PYTHON_BASEVERSION}/*/.debug"
#FILES_${PN}-python = "${libdir}/python${PYTHON_BASEVERSION}"
FILES_${PN}-dev += "${base_libdir}/*.so ${base_libdir}/*.la ${base_libdir}/pkgconfig/*"

CONFFILES_auditd += "${sysconfdir}/audit/audit.rules"

do_install_append() {
	rm -f ${D}/${libdir}/python${PYTHON_BASEVERSION}/site-packages/*.a
	rm -f ${D}/${libdir}/python${PYTHON_BASEVERSION}/site-packages/*.la

	# reuse auditd config
	[ ! -e ${D}/etc/default ] && mkdir ${D}/etc/default
	mv ${D}/etc/sysconfig/auditd ${D}/etc/default
	rmdir ${D}/etc/sysconfig/

	# replace init.d
	install -D -m 0750 ${THISDIR}/files/auditd ${D}/etc/init.d/auditd
	rm -rf ${D}/etc/rc.d

	if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
		# install systemd unit files
		install -d ${D}${systemd_unitdir}/system
		install -m 0644 ${THISDIR}/files/auditd.service ${D}${systemd_unitdir}/system

		install -d ${D}${sysconfdir}/tmpfiles.d/
		install -m 0644 ${THISDIR}/files/audit-volatile.conf ${D}${sysconfdir}/tmpfiles.d/
	fi

	# audit-2.5 doesn't install any rules by default, so we do that here
	mkdir -p ${D}/etc/audit ${D}/etc/audit/rules.d
	cp ${WORKDIR}/audit.rules ${D}/etc/audit/rules.d/audit.rules
	cp ${WORKDIR}/auditd.conf ${D}/etc/audit/auditd.conf

	chmod 750 ${D}/etc/audit ${D}/etc/audit/rules.d
	chmod 640 ${D}/etc/audit/auditd.conf ${D}/etc/audit/rules.d/audit.rules

	# Based on the audit.spec "Copy default rules into place on new installation"
	cp ${D}/etc/audit/rules.d/audit.rules ${D}/etc/audit/audit.rules
	rm -rf ${D}/lib/pkgconfig
}
