/*
 * Copyright (c) 2009, IETR/INSA of Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

/**
@brief Implementation of class Decoder
@author Jerome Gorin
@file Decoder.cpp
@version 1.0
@date 15/11/2010
*/

//------------------------------
#include <list>
#include <iostream>
#include <fstream>

#include "llvm/LLVMContext.h"
#include "llvm/Module.h"

#include "Jade/Decoder.h"
#include "Jade/Fifo/AbstractConnector.h"
#include "Jade/Core/Network.h"
#include "Jade/Configuration/ConfigurationEngine.h"
#include "Jade/Configuration/Configuration.h"
#include "Jade/Fifo/AbstractConnector.h"
#include "Jade/Jit/LLVMExecution.h"
#include "Jade/Serialize/IRUnwriter.h"
#include "Jade/Scheduler/RoundRobinScheduler.h"
//------------------------------

using namespace llvm;
using namespace std;

Decoder::Decoder(llvm::LLVMContext& C, Configuration* configuration): Context(C){
	//Set property of the decoder
	this->configuration = configuration;
	this->thread = NULL;
	this->executionEngine = NULL;
	this->scheduler = new RoundRobinScheduler(Context);
	this->instances = configuration->getInstances();
	this->fifo = configuration->getConnector();

	//Create a new module that contains the current decoder
	module = new Module("decoder", C);
}

Decoder::~Decoder (){
	delete scheduler;

	list<Actor*>::iterator it;
	for (it = specificActors.begin(); it != specificActors.end(); it++){
		delete *it;
	}

	delete module;
}

void Decoder::addInstance(Instance* instance){
	instances->insert(std::pair<std::string, Instance*>(instance->getId(), instance));
}

void Decoder::addSpecific(Actor* actor){
	specificActors.push_back(actor);
}




Instance* Decoder::getInstance(std::string name){
	map<string, Instance*>::iterator it;

	it = instances->find(name);

	if (it == instances->end()){
		return NULL;
	}

	return it->second;
}

void Decoder::start(){
	scheduler->setSource(stimulus);
	
	executionEngine = new LLVMExecution(Context, this);
	
	((RoundRobinScheduler*)scheduler)->setExternalFunctions(executionEngine);
	
	executionEngine->run();
}

void Decoder::stop(){
	executionEngine->stop();
	pthread_join (*thread, NULL);
	executionEngine->clear();
}

void Decoder::startInThread(pthread_t* thread){
	this->thread = thread;
	pthread_create( thread, NULL, &Decoder::threadStart, this );
}

void* Decoder::threadStart( void* args ){
	Decoder* decoder = static_cast<Decoder*>(args);
	decoder->start();
	return NULL;
}

void Decoder::setStimulus(std::string file){
	this->stimulus = file;
}
/*
void Decoder::setNetwork(Network* network){
	clearConnections();
	ReconfigurationScenario Configuration(getNetwork(), network);
		
	this->network = network;

	
}*/

/*
void Decoder::clearConnections(){
	list<Actor*>::iterator itActor;
	IRUnwriter unwriter(this);
	fifo->unsetConnections(this);

	for (itActor = specificActors.begin(); itActor != specificActors.end(); itActor++){
		list<Instance*>::iterator itInst;
		list<Instance*>* instances = (*itActor)->getInstances();

		for (itInst = instances->begin(); itInst != instances->end(); itInst++){
			unwriter.remove(*itInst);
		} 
	}

	specificActors.erase(specificActors.begin(), specificActors.end());
}*/