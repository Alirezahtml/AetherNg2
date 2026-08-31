/*
 ============================================================================
 Name        : hev-task-io.h
 Author      : Heiher <r@hev.cc>
 Copyright   : Copyright (c) 2018 everyone.
 Description : Task I/O operations
 ============================================================================
 */

#ifndef __HEV_TASK_IO_H__
#define __HEV_TASK_IO_H__

#include <sys/uio.h>
#include <sys/types.h>
#include "kern/task/hev-task.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef int (*HevTaskIOYielder) (HevTaskYieldType type, void *data);

int hev_task_io_open (const char *pathname, int flags, ...);
int hev_task_io_creat (const char *pathname, mode_t mode);
int hev_task_io_openat (int dirfd, const char *pathname, int flags, ...);
int hev_task_io_dup (int oldfd);
int hev_task_io_dup2 (int oldfd, int newfd);
ssize_t hev_task_io_read (int fd, void *buf, size_t count,
                          HevTaskIOYielder yielder, void *yielder_data);
ssize_t hev_task_io_readv (int fd, const struct iovec *iov, int iovcnt,
                           HevTaskIOYielder yielder, void *yielder_data);
ssize_t hev_task_io_write (int fd, const void *buf, size_t count,
                           HevTaskIOYielder yielder, void *yielder_data);
ssize_t hev_task_io_writev (int fd, const struct iovec *iov, int iovcnt,
                            HevTaskIOYielder yielder, void *yielder_data);
void hev_task_io_splice (int fd_a_i, int fd_a_o, int fd_b_i, int fd_b_o,
                         size_t buf_size, HevTaskIOYielder yielder,
                         void *yielder_data);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_TASK_IO_H__ */
