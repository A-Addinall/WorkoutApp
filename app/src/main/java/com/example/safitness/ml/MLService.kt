package com.example.safitness.ml

interface MLService {
    suspend fun generate(req: GenerateRequest): GenerateResponse
}
