/*
 ============================================================================
 Name        : hev-circular-buffer.h
 Author      : Heiher <r@hev.cc>
 Copyright   : Copyright (c) 2019 everyone.
 Description : Circular buffer
 ============================================================================
 */

#ifndef __HEV_CIRCULAR_BUFFER_H__
#define __HEV_CIRCULAR_BUFFER_H__

#include <sys/uio.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _HevCircularBuffer HevCircularBuffer;

HevCircularBuffer *hev_circular_buffer_new (size_t max_size);
HevCircularBuffer *hev_circular_buffer_ref (HevCircularBuffer *self);
void hev_circular_buffer_unref (HevCircularBuffer *self);
size_t hev_circular_buffer_get_max_size (HevCircularBuffer *self);
size_t hev_circular_buffer_get_use_size (HevCircularBuffer *self);
int hev_circular_buffer_reading (HevCircularBuffer *self, struct iovec *iov);
void hev_circular_buffer_read_finish (HevCircularBuffer *self, size_t size);
int hev_circular_buffer_writing (HevCircularBuffer *self, struct iovec *iov);
void hev_circular_buffer_write_finish (HevCircularBuffer *self, size_t size);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_CIRCULAR_BUFFER_H__ */
