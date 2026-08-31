/*
 ============================================================================
 Name        : hev-task-dns.h
 Author      : Heiher <r@hev.cc>
 Copyright   : Copyright (c) 2021 everyone.
 Description : DNS
 ============================================================================
 */

#ifndef __HEV_TASK_DNS_H__
#define __HEV_TASK_DNS_H__

#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>

#ifdef __cplusplus
extern "C" {
#endif

int hev_task_dns_getaddrinfo (const char *node, const char *service,
                              const struct addrinfo *hints,
                              struct addrinfo **res);
int hev_task_dns_getnameinfo (const struct sockaddr *addr, socklen_t addrlen,
                              char *node, socklen_t nodelen, char *service,
                              socklen_t servicelen, int flags);

#ifdef __cplusplus
}
#endif

#endif /* __HEV_TASK_DNS_H__ */
