!/bin/sh

cd ../src
jar cvmf Manifest.txt ../jkcemu.jar \
  lib/jkcemu_*.dll \
  jkcemu/*.class \
  jkcemu/audio/*.class \
  jkcemu/base/*.class \
  jkcemu/disk/*.class \
  jkcemu/emusys/*.class \
  jkcemu/emusys/a5105/*.class \
  jkcemu/emusys/ac1_llc2/*.class \
  jkcemu/emusys/etc/*.class \
  jkcemu/emusys/huebler/*.class \
  jkcemu/emusys/kc85/*.class \
  jkcemu/emusys/z1013/*.class \
  jkcemu/etc/*.class \
  jkcemu/filebrowser/*.class \
  jkcemu/image/*.class \
  jkcemu/joystick/*.class \
  jkcemu/print/*.class \
  jkcemu/programming/*.class \
  jkcemu/programming/assembler/*.class \
  jkcemu/programming/basic/*.class \
  jkcemu/text/*.class \
  jkcemu/tools/*.class \
  jkcemu/tools/calculator/*.class \
  jkcemu/tools/debugger/*.class \
  jkcemu/tools/hexdiff/*.class \
  jkcemu/tools/hexedit/*.class \
  z80emu/*.class \
  rom/a5105/*.bin \
  rom/ac1/*.bin \
  rom/bcs3/*.bin \
  rom/c80/*.bin \
  rom/huebler/*.bin \
  rom/kc85/*.bin \
  rom/kramermc/*.bin \
  rom/lc80/*.bin \
  rom/llc1/*.bin \
  rom/llc2/*.bin \
  rom/pcm/*.bin \
  rom/poly880/*.bin \
  rom/sc2/*.bin \
  rom/slc1/*.bin \
  rom/vcs80/*.bin \
  rom/z1013/*.bin \
  rom/z9001/*.bin \
  rom/z9001/*.gz \
  images/chess/*.png \
  images/debug/*.png \
  images/disk/*.png \
  images/edit/*.png \
  images/file/*.png \
  images/icon/*.png \
  images/nav/*.png \
  disks/harddisks.csv \
  disks/a5105/*.gz \
  disks/kc85/*.gz \
  disks/z1013/*.gz \
  disks/z9001/*.gz \
  help/*.htm \
  help/bcs3/*.htm \
  help/disk/*.htm \
  help/kramermc/*.htm \
  help/tips/*.htm \
  help/tools/*.htm \
  help/tools/basicc/*.htm \
  help/z1013/*.htm
