/*
 ============================================================================
 Name        : hev-memory-allocator-api.h
 Author      : Heiher <r@hev.cc>
 Copyright   : Copyright (c) 2018 everyone.
 Description : Memory allocator APIs
 ============================================================================
 */

#ifndef __HEV_MEMORY_ALLOCATOR_API_H__
#define __HEV_MEMORY_ALLOCATOR_API_H__

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

void *hev_malloc (size_t size);
void *hev_malloc0 (size_t size);
void *hev_calloc (size_t nmemb, size_t size);
void *hev_realloc (void *ptr, size_t size);
void hev_free (void *ptr);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_MEMORY_ALLOCATOR_API_H__ */
