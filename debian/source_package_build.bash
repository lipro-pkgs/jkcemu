#! /bin/bash
#
# debian/source_package_build.bash
# Part of the Debian package ‘jkcemu’.
#
# Copyright © 2017 Stephan Linz <linz@li-pro.net>
# Copyright © 2010–2016 Ben Finney <bignose@debian.org>
#
# This is free software; you may copy, modify, and/or distribute this
# work under the terms of the Apache License, version 2.0 as published
# by the Apache Software Foundation. No warranty expressed or implied.
# See the file ‘/usr/share/common-licenses/Apache-2.0’ for details.

# Common code for building Debian upstream source package.

# based on: https://wiki.debian.org/BenFinney/software/repack

working_dir="$(mktemp -d -t)"

exit_sigspecs="ERR EXIT SIGTERM SIGHUP SIGINT SIGQUIT"

function cleanup_exit() {
    exit_status=$?
    trap - $exit_sigspecs

    rm -rf "${working_dir}"
    printf "Cleaned up working directory ‘${working_dir}’\n"

    exit $exit_status
}
trap cleanup_exit $exit_sigspecs

package_name=$(dpkg-parsechangelog --show-field=Source)
release_version=$(dpkg-parsechangelog --show-field=Version)
upstream_version=$(printf "${release_version}" \
        | sed -e 's/^[[:digit:]]\+://' -e 's/[-][^-]\+$//')
upstream_dirname="${package_name}-${upstream_version}.orig"
upstream_tarball_basename="${package_name}_${upstream_version}.orig"

function extract_ziparchive_to_working_dir() {
    # Extract the specified tarball to the program's working directory.
    local tarball="$1"
    bsdtar -xf "${tarball}" --directory "${working_dir}"
}

function archive_working_dirname_to_tarball() {
    # Archive the specified directory, relative to the working directory,
    # to a new tarball of the specified name.
    local source_dirname="$1"
    local tarball="$2"
    XZ_OPT="--best" GZIP="--best" tar --directory "${working_dir}" \
        --owner=root --group=root --mode=a+rX \
        -caf "${tarball}" "${source_dirname}"
}


# Local variables:
# coding: utf-8
# mode: shell-script
# indent-tabs-mode: nil
# End:
# vim: fileencoding=utf-8 filetype=bash expandtab :
