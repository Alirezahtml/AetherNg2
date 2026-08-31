/*
 ============================================================================
 Name        : hev-object-atomic.h
 Author      : hev <r@hev.cc>
 Copyright   : Copyright (c) 2024 hev
 Description : Object with atomic ref-count
 ============================================================================
 */

#ifndef __HEV_OBJECT_ATOMIC_H__
#define __HEV_OBJECT_ATOMIC_H__

#include "lib/object/hev-object.h"

#ifdef __cplusplus
extern "C" {
#endif

#define HEV_OBJECT_ATOMIC(p) ((HevObjectAtomic *)p)
#define HEV_OBJECT_ATOMIC_CLASS(p) ((HevObjectAtomicClass *)p)
#define HEV_OBJECT_ATOMIC_TYPE (hev_object_atomic_class ())

typedef struct _HevObjectAtomic HevObjectAtomic;
typedef struct _HevObjectAtomicClass HevObjectAtomicClass;

struct _HevObjectAtomic
{
    HevObject base;
};

struct _HevObjectAtomicClass
{
    HevObjectClass base;
};

HevObjectClass *hev_object_atomic_class (void);
int hev_object_atomic_construct (HevObjectAtomic *self);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_OBJECT_ATOMIC_H__ */
