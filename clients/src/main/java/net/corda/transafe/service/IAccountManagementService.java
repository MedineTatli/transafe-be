package net.corda.transafe.service;

import net.corda.transafe.request.HandShakeRequest;
import net.corda.transafe.response.HandShakeResponse;

import java.util.concurrent.ExecutionException;

public interface IAccountManagementService {
    HandShakeResponse handShake(HandShakeRequest request) throws ExecutionException, InterruptedException;
}
