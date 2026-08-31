# Build

rwildcard=$(foreach d,$(wildcard $1*), \
          $(call rwildcard,$d/,$2) \
          $(filter $(subst *,%,$2),$d))

SRCFILES=$(call rwildcard,$(SRCDIR)/,*.c *.S)

ifeq ($(REV_ID),)
  REV_ID=9a06bc6
endif
VERSION_CFLAGS=-DCOMMIT_ID=\"$(REV_ID)\"
