/*
 ============================================================================
 Name        : hev-task-io-poll.h
 Author      : Heiher <r@hev.cc>
 Copyright   : Copyright (c) 2017 - 2018 everyone.
 Description : I/O Poll
 ============================================================================
 */

#ifndef __HEV_TASK_IO_POLL_H__
#define __HEV_TASK_IO_POLL_H__

#include <poll.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct pollfd HevTaskIOPollFD;

int hev_task_io_poll (HevTaskIOPollFD fds[], unsigned int nfds, int timeout);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_TASK_IO_POLL_H__ */
