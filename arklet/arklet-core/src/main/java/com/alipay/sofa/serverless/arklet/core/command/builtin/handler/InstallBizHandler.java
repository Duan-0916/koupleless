/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.serverless.arklet.core.command.builtin.handler;

import java.util.Set;

import com.alipay.sofa.ark.api.ClientResponse;
import com.alipay.sofa.ark.api.ResponseCode;
import com.alipay.sofa.ark.common.util.BizIdentityUtils;
import com.alipay.sofa.ark.spi.model.BizInfo;
import com.alipay.sofa.serverless.arklet.core.command.builtin.BuiltinCommand;
import com.alipay.sofa.serverless.arklet.core.command.builtin.handler.InstallBizHandler.Input;
import com.alipay.sofa.serverless.arklet.core.command.coordinate.BizCommandCoordinator;
import com.alipay.sofa.serverless.arklet.core.command.meta.AbstractCommandHandler;
import com.alipay.sofa.serverless.arklet.core.command.meta.Command;
import com.alipay.sofa.serverless.arklet.core.command.meta.InputMeta;
import com.alipay.sofa.serverless.arklet.core.command.meta.Output;
import com.alipay.sofa.serverless.arklet.core.common.exception.ArkletRuntimeException;
import com.alipay.sofa.serverless.arklet.core.common.exception.CommandValidationException;
import lombok.Getter;
import lombok.Setter;

/**
 * @author mingmen
 * @date 2023/6/8
 */
public class InstallBizHandler extends AbstractCommandHandler<Input, Set<BizInfo>> {

    @Override
    public Output<Set<BizInfo>> handle(Input input) {
        boolean conflict = BizCommandCoordinator.existBizProcessing(input.getBizName(),
            input.getBizVersion());
        if (conflict) {
            return Output
                .ofFailed(ResponseCode.FAILED.name()
                          + ":"
                          + String.format(
                              "%s install conflict, exist unfinished command for this biz",
                              BizIdentityUtils.generateBizIdentity(input.getBizName(),
                                  input.getBizVersion())));
        }
        try {
            BizCommandCoordinator.putBizExecution(input.getBizName(), input.getBizVersion(),
                command());
            String bizFile = input.getArkBizFilePath();
            ClientResponse res = getOperationService().install(bizFile);
            if (ResponseCode.SUCCESS.equals(res.getCode())) {
                return Output.ofSuccess(res.getBizInfos());
            } else {
                return Output.ofFailed(res.getCode().name() + ":" + res.getMessage());
            }
        } catch (Throwable e) {
            throw new ArkletRuntimeException(e);
        } finally {
            BizCommandCoordinator.popBizExecution(input.bizName, input.getBizVersion());
        }
    }

    @Override
    public Command command() {
        return BuiltinCommand.INSTALL_BIZ;
    }

    @Override
    public void validate(Input input) throws CommandValidationException {
        notBlank(input.getBizName(), "bizName should not be blank");
        notBlank(input.getBizVersion(), "bizVersion should not be blank");
        notBlank(input.getArkBizFilePath(), "arkBizFilePath should not be blank");
    }

    @Getter
    @Setter
    public static class Input extends InputMeta {
        private String arkBizFilePath;
        private String bizName;
        private String bizVersion;
        private String downloadUrl;
    }

}
