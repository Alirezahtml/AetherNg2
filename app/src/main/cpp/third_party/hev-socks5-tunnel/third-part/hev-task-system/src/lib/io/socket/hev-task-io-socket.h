/*
 ============================================================================
 Name        : hev-task-io-socket.h
 Author      : Heiher <r@hev.cc>
 Copyright   : Copyright (c) 2018 everyone.
 Description : Task socket I/O operations
 ============================================================================
 */

#ifndef __HEV_TASK_IO_SOCKET_H__
#define __HEV_TASK_IO_SOCKET_H__

#include <sys/socket.h>
#include "lib/io/basic/hev-task-io.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef MSG_WAITFORONE
struct mmsghdr
{
    struct msghdr msg_hdr;
    unsigned int msg_len;
};
#endif

int hev_task_io_socket_socket (int domain, int type, int protocol);
int hev_task_io_socket_socketpair (int domain, int type, int protocol,
                                   int socket_vector[2]);
int hev_task_io_socket_connect (int fd, const struct sockaddr *addr,
                                socklen_t addr_len, HevTaskIOYielder yielder,
                                void *yielder_data);
int hev_task_io_socket_accept (int fd, struct sockaddr *addr,
                               socklen_t *addr_len, HevTaskIOYielder yielder,
                               void *yielder_data);
ssize_t hev_task_io_socket_recv (int fd, void *buf, size_t len, int flags,
                                 HevTaskIOYielder yielder, void *yielder_data);
ssize_t hev_task_io_socket_send (int fd, const void *buf, size_t len, int flags,
                                 HevTaskIOYielder yielder, void *yielder_data);
ssize_t hev_task_io_socket_recvfrom (int fd, void *buf, size_t len, int flags,
                                     struct sockaddr *addr, socklen_t *addr_len,
                                     HevTaskIOYielder yielder,
                                     void *yielder_data);
ssize_t hev_task_io_socket_sendto (int fd, const void *buf, size_t len,
                                   int flags, const struct sockaddr *addr,
                                   socklen_t addr_len, HevTaskIOYielder yielder,
                                   void *yielder_data);
ssize_t hev_task_io_socket_recvmsg (int fd, struct msghdr *msg, int flags,
                                    HevTaskIOYielder yielder,
                                    void *yielder_data);
ssize_t hev_task_io_socket_sendmsg (int fd, const struct msghdr *msg, int flags,
                                    HevTaskIOYielder yielder,
                                    void *yielder_data);
int hev_task_io_socket_recvmmsg (int fd, void *msgv, unsigned int n, int flags,
                                 HevTaskIOYielder yielder, void *yielder_data);
int hev_task_io_socket_sendmmsg (int fd, void *msgv, unsigned int n, int flags,
                                 HevTaskIOYielder yielder, void *yielder_data);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_TASK_IO_SOCKET_H__ */
